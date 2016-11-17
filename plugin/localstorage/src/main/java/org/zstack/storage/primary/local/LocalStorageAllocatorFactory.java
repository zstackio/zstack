package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
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
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageAllocatorFactory implements PrimaryStorageAllocatorStrategyFactory, Component,
        HostAllocatorFilterExtensionPoint, PrimaryStorageAllocatorStrategyExtensionPoint, PrimaryStorageAllocatorFlowNameSetter,
        HostAllocatorStrategyExtensionPoint {
    private CLogger logger = Utils.getLogger(LocalStorageAllocatorFactory.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

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
        if (VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            List<String> huuids = CollectionUtils.transformToList(candidates, new Function<String, HostVO>() {
                @Override
                public String call(HostVO arg) {
                    return arg.getUuid();
                }
            });

            SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
            q.select(LocalStorageHostRefVO_.hostUuid, LocalStorageHostRefVO_.availableCapacity, LocalStorageResourceRefVO_.primaryStorageUuid);
            q.add(LocalStorageHostRefVO_.hostUuid, Op.IN, huuids);
            List<Tuple> ts = q.listTuple();

            final List<String> toRemoveHuuids = new ArrayList<String>();
            for (Tuple t : ts) {
                String huuid = t.get(0, String.class);
                long cap = t.get(1, Long.class);
                String psUuid = t.get(2, String.class);
                if (cap < ratioMgr.calculateByRatio(psUuid, spec.getDiskSize())) {
                    toRemoveHuuids.add(huuid);
                }
            }

            if (!toRemoveHuuids.isEmpty()) {
                logger.debug(String.format("local storage filters out hosts%s, because they don't have required disk capacity[%s bytes]", toRemoveHuuids, spec.getDiskSize()));

                candidates = CollectionUtils.transformToList(candidates, new Function<HostVO, HostVO>() {
                    @Override
                    public HostVO call(HostVO arg) {
                        return toRemoveHuuids.contains(arg.getUuid()) ? null : arg;
                    }
                });

                if (candidates.isEmpty()) {
                    throw new OperationFailureException(errf.instantiateErrorCode(HostAllocatorError.NO_AVAILABLE_HOST,
                            String.format("the local primary storage has no hosts with enough disk capacity[%s bytes] required by the vm[uuid:%s]",
                                    spec.getDiskSize(), spec.getVmInstance().getUuid())
                    ));
                }
            }
        } else if (VmOperation.Start.toString().equals(spec.getVmOperation())) {
            final LocalStorageResourceRefVO ref = dbf.findByUuid(spec.getVmInstance().getRootVolumeUuid(), LocalStorageResourceRefVO.class);
            if (ref != null) {
                candidates = CollectionUtils.transformToList(candidates, new Function<HostVO, HostVO>() {
                    @Override
                    public HostVO call(HostVO arg) {
                        return arg.getUuid().equals(ref.getHostUuid()) ? arg : null;
                    }
                });

                if (candidates.isEmpty()) {
                    throw new OperationFailureException(errf.instantiateErrorCode(HostAllocatorError.NO_AVAILABLE_HOST,
                            String.format("the vm[uuid: %s] using local primary storage can only be started on the host[uuid: %s], but the host is either not having enough CPU/memory or in" +
                                    " the state[Enabled] or status[Connected] to start the vm", spec.getVmInstance().getUuid(), ref.getHostUuid())
                    ));
                }
            }
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
            q.select(PrimaryStorageVO_.type);
            q.add(PrimaryStorageVO_.uuid, Op.EQ, msg.getRequiredPrimaryStorageUuid());
            String type = q.findValue();
            if (LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(type)) {
                allocatorType = LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
            }
        } else if (msg.getRequiredHostUuid() != null) {
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
}
