package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostAllocatorStrategyExtensionPoint;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.PrimaryStorageCapacityChecker;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.storage.snapshot.SnapshotDeletionExtensionPoint;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageAllocatorFactory implements PrimaryStorageAllocatorStrategyFactory, Component,
        HostAllocatorFilterExtensionPoint, PrimaryStorageAllocatorStrategyExtensionPoint, PrimaryStorageAllocatorFlowNameSetter,
        HostAllocatorStrategyExtensionPoint, SnapshotDeletionExtensionPoint, PSReserveCapacityExtensionPoint {
    private final static CLogger logger = Utils.getLogger(LocalStorageAllocatorFactory.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    public static PrimaryStorageAllocatorStrategyType type = new PrimaryStorageAllocatorStrategyType(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);

    private List<String> allocatorFlowNames;
    private FlowChainBuilder builder = new FlowChainBuilder();
    private LocalStorageAllocatorStrategy strategy;

    @Override
    public PrimaryStorageAllocatorStrategyType getPrimaryStorageAllocatorStrategyType() {
        return type;
    }

    @Override
    public PrimaryStorageAllocatorStrategy getPrimaryStorageAllocatorStrategy() {
        return strategy;
    }

    public List<String> getAllocatorFlowNames() {
        return allocatorFlowNames;
    }

    public void setAllocatorFlowNames(List<String> allocatorFlowNames) {
        this.allocatorFlowNames = allocatorFlowNames;
    }

    @Override
    public boolean start() {
        builder.setFlowClassNames(allocatorFlowNames).construct();
        strategy = new LocalStorageAllocatorStrategy(builder);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec) {
        long reservedCapacity = SizeUtils.sizeStringToBytes(PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value());

        if (VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            List<String> huuids = getNeedCheckHostLocalStorageList(candidates, spec);
            if (huuids.isEmpty()) {
                return candidates;
            }

            SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
            q.add(LocalStorageHostRefVO_.hostUuid, Op.IN, huuids);
            if (!spec.getRequiredPrimaryStorageUuids().isEmpty()) {
                q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.IN, spec.getRequiredPrimaryStorageUuids());
            }
            List<LocalStorageHostRefVO> refs = q.list();

            final Set<String> toRemoveHuuids = new HashSet<>();
            final Set<String> toAddHuuids = new HashSet<>();
            for (LocalStorageHostRefVO ref : refs) {
                String huuid = ref.getHostUuid();
                String psUuid = ref.getPrimaryStorageUuid();
                // check primary storage capacity and host physical capacity
                boolean capacityChecked = PrimaryStorageCapacityChecker.New(psUuid,
                        ref.getAvailableCapacity(), ref.getTotalPhysicalCapacity(), ref.getAvailablePhysicalCapacity())
                        .checkRequiredSize(spec.getDiskSize());

                if (!capacityChecked) {
                    addHostPrimaryStorageBlacklist(huuid, psUuid, spec);
                    toRemoveHuuids.add(huuid);
                } else {
                    toAddHuuids.add(huuid);
                }
            }
            // for more than one local storage, maybe one of it fit the requirement
            toRemoveHuuids.removeAll(toAddHuuids);
            if (!toRemoveHuuids.isEmpty()) {
                logger.debug(String.format("local storage filters out hosts%s, because they don't have required disk capacity[%s bytes]",
                        toRemoveHuuids, spec.getDiskSize()));

                candidates = CollectionUtils.transformToList(candidates, new Function<HostVO, HostVO>() {
                    @Override
                    public HostVO call(HostVO arg) {
                        return toRemoveHuuids.contains(arg.getUuid()) ? null : arg;
                    }
                });

                if (candidates.isEmpty()) {
                    throw new OperationFailureException(err(HostAllocatorError.NO_AVAILABLE_HOST,
                            "the local primary storage has no hosts with enough disk capacity[%s bytes] required by the vm[uuid:%s]",
                            spec.getDiskSize(), spec.getVmInstance().getUuid()
                    ));
                }
            }
        } else if (VmOperation.Start.toString().equals(spec.getVmOperation())) {
            checkLocalStorageForVmStart(spec.getVmInstance(), candidates);
        }


        /*
        else if (VmOperation.Migrate.toString().equals(spec.getVmOperation())) {
            final LocalStorageResourceRefVO ref = dbf.findByUuid(spec.getVmInstance().getRootVolumeUuid(), LocalStorageResourceRefVO.class);
            if (ref != null) {
                throw new OperationFailureException(errf.instantiateErrorCode(HostAllocatorError.NO_AVAILABLE_HOST,
                        String.format("the vm[uuid: %s] cannot migrate because of using local primary storage on the host[uuid: %s]",
                                spec.getVmInstance().getUuid(), ref.getHostUuid())
                ));
            }
        }
        */

        return candidates;
    }

    private void checkLocalStorageForVmStart(VmInstanceInventory vm, List<HostVO> candidates) {
        final List<String> localPS = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .listValues();
        if (localPS.isEmpty()) {
            return;
        }

        for (VolumeInventory volume : vm.getAllVolumes()) {
            if (!localPS.contains(volume.getPrimaryStorageUuid())) {
                continue;
            }

            final String hostUuid = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, volume.getUuid())
                    .select(LocalStorageResourceRefVO_.hostUuid)
                    .findValue();
            if (hostUuid != null) {
                candidates.removeIf(h -> !hostUuid.equals(h.getUuid()));
                if (candidates.isEmpty()) {
                    throw new OperationFailureException(err(HostAllocatorError.NO_AVAILABLE_HOST,
                            "the vm[uuid: %s] using local primary storage can only be started on the host[uuid: %s], but the host is either not having enough CPU/memory/GPU/VFNIC or in" +
                                    " the state[Enabled] or status[Connected] to start the vm", vm.getUuid(), hostUuid
                    ));
                }

                // A VM with local disk can only run on this host
                break;
            }
        }
    }

    /**
     * @return hostUuid list
     * <p>
     * Just check it :
     * The current cluster is mounted only local storage
     * Specified local storage
     * <p>
     * Negative impact
     * In the case of local + non-local and no ps specified (non-local is Disconnected/Disabled, or non-local capacity not enough), the allocated host may not have enough disks
     */
    private List<String> getNeedCheckHostLocalStorageList(List<HostVO> candidates, HostAllocatorSpec spec) {
        boolean isRequireNonLocalStorage = spec.getRequiredPrimaryStorageUuids()
                .stream().noneMatch(LocalStorageUtils::isLocalStorage);
        Map<String, List<String>> grouped = candidates.stream().collect(
                Collectors.groupingBy(
                        HostVO::getClusterUuid,
                        Collectors.mapping(HostVO::getUuid, Collectors.toList())
                )
        );

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            boolean isOnlyAttachedLocalStorage = LocalStorageUtils.isOnlyAttachedLocalStorage(entry.getKey());
            if (!isOnlyAttachedLocalStorage && (isRequireNonLocalStorage || spec.isDryRun())) {
                continue;
            }

            result.addAll(entry.getValue());
        }

        return result;
    }

    private void addHostPrimaryStorageBlacklist(String hostUuid, String psUuid, HostAllocatorSpec spec) {
        Map<String, Set<String>> host2PrimaryStorageBlacklist = spec.getHost2PrimaryStorageBlacklist();
        Set<String> psList = host2PrimaryStorageBlacklist.get(hostUuid);

        if (psList == null) {
            psList = new HashSet<>();
        }

        psList.add(psUuid);
        host2PrimaryStorageBlacklist.put(hostUuid, psList);
    }

    @Override
    public String getPrimaryStorageAllocatorStrategyName(final AllocatePrimaryStorageMsg msg) {
        String allocatorType = null;
        if (msg.getExcludeAllocatorStrategies() != null
                && msg.getExcludeAllocatorStrategies().contains(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY)
                ) {
            allocatorType = null;
        } else if (LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY.equals(msg.getAllocationStrategy())) {
            allocatorType = LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
        } else if (msg.getRequiredPrimaryStorageUuid() != null) {
            SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
            q.add(PrimaryStorageVO_.type, Op.EQ, LocalStorageConstants.LOCAL_STORAGE_TYPE);
            q.add(PrimaryStorageVO_.uuid, Op.EQ, msg.getRequiredPrimaryStorageUuid());
            if (q.isExists()) {
                allocatorType = LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
            }
        } else if (msg.getRequiredHostUuid() != null) {

            if (msg.getAllocationStrategy() != null && !LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY.equals(msg.getAllocationStrategy())) {
                return null;
            }

            allocatorType = new Callable<String>() {
                @Override
                @Transactional(readOnly = true)
                public String call() {
                    String sql = "select ps.type" +
                            " from PrimaryStorageVO ps, PrimaryStorageClusterRefVO ref, HostVO host" +
                            " where ps.uuid = ref.primaryStorageUuid" +
                            " and ref.clusterUuid = host.clusterUuid" +
                            " and host.uuid = :huuid";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("huuid", msg.getRequiredHostUuid());
                    List<String> types = q.getResultList();
                    for (String type : types) {
                        if (type.equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
                            return LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
                        }
                    }
                    return null;
                }
            }.call();
        }

        return allocatorType;
    }

    @Override
    public String getAllocatorStrategy(HostInventory host) {
        if (host != null && !"KVM".equals(host.getHypervisorType())) {
            return null;
        }
        return LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
    }

    @Override
    public String getHostAllocatorStrategyName(HostAllocatorSpec spec) {
        if (!VmOperation.Migrate.toString().equals(spec.getVmOperation())) {
            return null;
        }

        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.select(PrimaryStorageVO_.type);
        q.add(PrimaryStorageVO_.uuid, Op.EQ, spec.getVmInstance().getRootVolume().getPrimaryStorageUuid());
        String type = q.findValue();
        if (!LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(type)) {
            return null;
        }

        return LocalStorageConstants.LOCAL_STORAGE_MIGRATE_VM_ALLOCATOR_TYPE;
    }

    @Override
    public String filterErrorReason() {
        return Platform.i18n("localstorage allocator failed");
    }

    @Override
    public String getHostUuidByResourceUuid(String primaryStorageUuid, String resUuid) {
        if (LocalStorageUtils.isLocalStorage(primaryStorageUuid)) {
            return LocalStorageUtils.getHostUuidByResourceUuid(resUuid);
        }
        return null;
    }

    @Override
    public String getRequireInstallUrl(AllocatePrimaryStorageSpaceMsg msg) {
        String requireInstallUrl = null;
        String hostUuid = null;

        if (msg.getSystemTags() != null) {
            for (String stag : msg.getSystemTags()) {
                    if (LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.isMatch(stag)) {
                    hostUuid = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.getTokenByTag(
                            stag,
                            LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN
                    );
                    requireInstallUrl = hostUuid;
                    break;
                }
            }

            if (hostUuid == null) {
                throw new OperationFailureException(argerr("To create data volume on the local primary storage, you must specify the host that" +
                                " the data volume is going to be created using the system tag [%s]",
                        LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.getTagFormat()));
            }
        }

        return requireInstallUrl;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveCapacity(String installUrl, long size, String psUuid){
        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(psUuid, PrimaryStorageVO.class);

        String hostUuid = installUrl;

        String sql = "select ref" +
                " from LocalStorageHostRefVO ref" +
                " where ref.hostUuid = :huuid" +
                " and ref.primaryStorageUuid = :psUuid";
        TypedQuery<LocalStorageHostRefVO> q = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("psUuid", psUuid);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<LocalStorageHostRefVO> refs = q.getResultList();

        if (refs.isEmpty()) {
            String errInfo = String.format("cannot find host[uuid: %s] of local primary storage[uuid: %s]",
                    hostUuid, primaryStorageVO.getUuid());
            throw new CloudRuntimeException(errInfo);
        }

        LocalStorageHostRefVO ref = refs.get(0);

        if (!physicalCapacityMgr.checkCapacityByRatio(
                primaryStorageVO.getUuid(),
                ref.getTotalPhysicalCapacity(),
                ref.getAvailablePhysicalCapacity())) {
            throw new OperationFailureException(operr("cannot reserve enough space for primary storage[uuid: %s]" +
                    " on host[uuid: %s], not enough physical capacity", primaryStorageVO.getUuid(), hostUuid));
        }

        LocalStorageHostCapacityStruct s = new LocalStorageHostCapacityStruct();
        s.setLocalStorage(PrimaryStorageInventory.valueOf(primaryStorageVO));
        s.setHostUuid(ref.getHostUuid());
        s.setSizeBeforeOverProvisioning(size);
        s.setSize(size);

        for (LocalStorageReserveHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(
                LocalStorageReserveHostCapacityExtensionPoint.class)) {
            ext.beforeReserveLocalStorageCapacityOnHost(s);
        }

        long avail = ref.getAvailableCapacity() - s.getSize();
        if (avail < 0) {
            throw new OperationFailureException(operr("host[uuid: %s] of local primary storage[uuid: %s] " +
                            "doesn't have enough capacity [current: %s bytes, needed: %s]",
                    hostUuid, primaryStorageVO.getUuid(), ref.getAvailableCapacity(), size));
        }

        logCapacityChange(psUuid, hostUuid, ref.getAvailableCapacity(), avail);
        ref.setAvailableCapacity(avail);
        dbf.getEntityManager().merge(ref);
    }

    public static void logCapacityChange(String psUuid, String hostUuid, long before, long after) {
        if (logger.isTraceEnabled()) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            int index = 0;
            String fileName = LocalStorageUtils.class.getSimpleName() + ".java";
            for (int i = 0; i < stackTraceElements.length; i++) {
                if (fileName.equals(stackTraceElements[i].getFileName())) {
                    index = i;
                }
            }
            StackTraceElement caller = stackTraceElements[index + 1];
            logger.trace(String.format("[Local Storage Capacity] %s:%s:%s changed the capacity of the local storage[uuid:%s], host[uuid:%s] as:\n" +
                            "available: %s --> %s\n",
                    caller.getFileName(), caller.getMethodName(), caller.getLineNumber(), psUuid, hostUuid, before, after));
        }
    }

    @Override
    public void releaseCapacity(String installUrl, long size, String psUuid){
        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
        String hostUuid = installUrl;

        String sql = "select ref from LocalStorageHostRefVO ref where ref.hostUuid = :huuid and ref.primaryStorageUuid = :primaryStorageUuid";
        TypedQuery<LocalStorageHostRefVO> q = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("primaryStorageUuid", primaryStorageVO.getUuid());
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<LocalStorageHostRefVO> refs = q.getResultList();

        if (refs.isEmpty()) {
            throw new CloudRuntimeException(String.format("cannot find host[uuid: %s] of local primary storage[uuid: %s]",
                    hostUuid, primaryStorageVO.getUuid()));
        }

        LocalStorageHostRefVO ref = refs.get(0);

        LocalStorageHostCapacityStruct s = new LocalStorageHostCapacityStruct();
        s.setSizeBeforeOverProvisioning(size);
        s.setHostUuid(hostUuid);
        s.setLocalStorage(PrimaryStorageInventory.valueOf(primaryStorageVO));
        s.setSize(size);

        for (LocalStorageReturnHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(
                LocalStorageReturnHostCapacityExtensionPoint.class)) {
            ext.beforeReturnLocalStorageCapacityOnHost(s);
        }

        logCapacityChange(primaryStorageVO.getUuid(), hostUuid, ref.getAvailableCapacity(), ref.getAvailableCapacity() + s.getSize());
        ref.setAvailableCapacity(ref.getAvailableCapacity() + s.getSize());
        dbf.getEntityManager().merge(ref);
    }
}
