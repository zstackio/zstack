package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmAllocatePrimaryStorageFlow;
import org.zstack.compute.vm.VmAllocatePrimaryStorageForAttachingDiskFlow;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageFactory implements PrimaryStorageFactory, Component,
        MarshalVmOperationFlowExtensionPoint, HostDeleteExtensionPoint, VmAttachVolumeExtensionPoint,
        GetAttachableVolumeExtensionPoint {
    private final static CLogger logger = Utils.getLogger(LocalStorageFactory.class);
    public static PrimaryStorageType type = new PrimaryStorageType(LocalStorageConstants.LOCAL_STORAGE_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private Map<String, LocalStorageBackupStorageMediator> backupStorageMediatorMap = new HashMap<String, LocalStorageBackupStorageMediator>();

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg msg) {
        vo.setMountPath(msg.getUrl());
        vo = dbf.persistAndRefresh(vo);
        return PrimaryStorageInventory.valueOf(vo);
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new LocalStorageBase(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return PrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, PrimaryStorageVO.class));
    }

    private String makeMediatorKey(String hvType, String bsType) {
        return hvType + "-" + bsType;
    }

    public LocalStorageBackupStorageMediator getBackupStorageMediator(String hvType, String bsType) {
        LocalStorageBackupStorageMediator m = backupStorageMediatorMap.get(makeMediatorKey(hvType, bsType));
        if (m == null) {
            throw new CloudRuntimeException(String.format("no LocalStorageBackupStorageMediator supporting hypervisor[%s] and backup storage[%s] ",
                    hvType, bsType));
        }

        return m;
    }

    @Override
    public boolean start() {
        for (LocalStorageBackupStorageMediator m : pluginRgty.getExtensionList(LocalStorageBackupStorageMediator.class)) {
            for (HypervisorType hvType : m.getSupportedHypervisorTypes()) {
                String key = makeMediatorKey(hvType.toString(), m.getSupportedBackupStorageType().toString());
                LocalStorageBackupStorageMediator old = backupStorageMediatorMap.get(key);
                if (old != null) {
                    throw new CloudRuntimeException(String.format("duplicate LocalStorageBackupStorageMediator[%s, %s] for hypervisor type[%s] and backup storage type[%s]",
                            m, old, hvType, m.getSupportedBackupStorageType()));
                }

                backupStorageMediatorMap.put(key, m);
            }
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Transactional(readOnly = true)
    private String getLocalStorageInCluster(String clusterUuid) {
        String sql = "select pri.uuid from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref where pri.uuid = ref.primaryStorageUuid and ref.clusterUuid = :cuuid and pri.type = :ptype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        List<String> ret = q.getResultList();
        if (ret.isEmpty()) {
            return null;
        }

        return ret.get(0);
    }

    @Override
    public Flow marshalVmOperationFlow(String previousFlowName, String nextFlowName, FlowChain chain, VmInstanceSpec spec) {
        if (VmAllocatePrimaryStorageFlow.class.getName().equals(nextFlowName)) {
            if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
                if (getLocalStorageInCluster(spec.getDestHost().getClusterUuid()) != null) {
                    return new LocalStorageAllocateCapacityFlow();
                }
            }
        } else if (spec.getCurrentVmOperation() == VmOperation.AttachVolume) {
            VolumeInventory volume = spec.getDestDataVolumes().get(0);
            if (VolumeStatus.NotInstantiated.toString().equals(volume.getStatus()) && VmAllocatePrimaryStorageForAttachingDiskFlow.class.getName().equals(nextFlowName)) {
                SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
                q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, spec.getVmInventory().getRootVolumeUuid());
                if (q.isExists()) {
                    return new LocalStorageAllocateCapacityForAttachingVolumeFlow();
                }
            }
        }

        return null;
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {
    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
    }

    @Override
    public void afterDeleteHost(final HostInventory inventory) {
        final String priUuid = getLocalStorageInCluster(inventory.getClusterUuid());
        if (priUuid != null) {
            RemoveHostFromLocalStorageMsg msg = new RemoveHostFromLocalStorageMsg();
            msg.setPrimaryStorageUuid(priUuid);
            msg.setHostUuid(inventory.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, priUuid);
            bus.send(msg, new CloudBusCallBack() {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to remove host[uuid:%s] from local primary storage[uuid:%s], %s",
                                inventory.getUuid(), priUuid, reply.getError()));
                    } else {
                        logger.debug(String.format("removed host[uuid:%s] from local primary storage[uuid:%s]",
                                inventory.getUuid(), priUuid));
                    }
                }
            });
        }
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, final VolumeInventory volume) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.select(LocalStorageResourceRefVO_.hostUuid);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.IN, list(vm.getRootVolumeUuid(), volume.getUuid()));
        List<String> huuids = q.listValue();

        if (huuids.size() < 2) {
            return;
        }

        String rootHost = huuids.get(0);
        String dataHost = huuids.get(1);

        if (!rootHost.equals(dataHost)) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot attach the data volume[uuid:%s] to the vm[uuid:%s]. Both vm's root volume and the data volume are" +
                            " on local primary storage, but they are on different hosts. The root volume[uuid:%s] is on the host[uuid:%s] but the data volume[uuid: %s]" +
                            " is on the host[uuid: %s]", volume.getUuid(), vm.getUuid(), vm.getRootVolumeUuid(), rootHost, volume.getUuid(), dataHost)
            ));
        }
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {

    }

    @Override
    @Transactional(readOnly = true)
    public List<VolumeVO> returnAttachableVolumes(VmInstanceInventory vm, List<VolumeVO> candidates) {
        // find instantiated volumes
        List<String> volUuids = CollectionUtils.transformToList(candidates, new Function<String, VolumeVO>() {
            @Override
            public String call(VolumeVO arg) {
                return VolumeStatus.Ready == arg.getStatus() ? arg.getUuid() : null;
            }
        });

        String hostUuid =  VmInstanceState.Running.toString().equals(vm.getState()) ? vm.getHostUuid() : vm.getLastHostUuid();
        if (hostUuid == null) {
            throw new CloudRuntimeException(String.format("hostUuid is null; vm[uuid: %s, state: %s, hostUuid: %s, lastHostUuid: %s]",
                    vm.getUuid(), vm.getState(), vm.getHostUuid(), vm.getLastHostUuid()));
        }

        String sql = "select ref.resourceUuid from LocalStorageResourceRefVO ref where ref.resourceUuid in (:uuids) and ref.resourceType = :rtype" +
                " and ref.hostUuid != :huuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuids", volUuids);
        q.setParameter("huuid", hostUuid);
        q.setParameter("rtype", VolumeVO.class.getSimpleName());
        final List<String> toExclude = q.getResultList();

        if (!toExclude.isEmpty()) {
            candidates = CollectionUtils.transformToList(candidates, new Function<VolumeVO, VolumeVO>() {
                @Override
                public VolumeVO call(VolumeVO arg) {
                    return toExclude.contains(arg.getUuid()) ? null : arg;
                }
            });
        }

        return candidates;
    }
}
