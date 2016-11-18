package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.*;
import org.zstack.header.cluster.ReportHostCapacityMessage;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.APIGetCandidateBackupStorageForCreatingImageMsg;
import org.zstack.header.image.APIGetCandidateBackupStorageForCreatingImageReply;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmAbnormalLifeCycleExtensionPoint;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Callable;

import static org.zstack.utils.CollectionDSL.list;

public class HostAllocatorManagerImpl extends AbstractService implements HostAllocatorManager, VmAbnormalLifeCycleExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostAllocatorManagerImpl.class);

    private Map<String, HostAllocatorStrategyFactory> factories = Collections.synchronizedMap(new HashMap<String, HostAllocatorStrategyFactory>());
    private Map<String, List<String>> backupStoragePrimaryStorageMetrics;
    private Map<String, List<String>> primaryStorageBackupStorageMetrics = new HashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private HostCapacityReserveManager reserveMgr;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;
    @Autowired
    private HostCpuOverProvisioningManager cpuRatioMgr;
    @Autowired
    private ErrorFacade errf;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AllocateHostMsg) {
            handle((AllocateHostMsg) msg);
        } else if (msg instanceof ReportHostCapacityMessage) {
            handle((ReportHostCapacityMessage) msg);
        } else if (msg instanceof ReturnHostCapacityMsg) {
            handle((ReturnHostCapacityMsg) msg);
        } else if (msg instanceof RecalculateHostCapacityMsg) {
            handle((RecalculateHostCapacityMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional(readOnly = true)
    private void handle(APIGetCandidateBackupStorageForCreatingImageMsg msg) {
        PrimaryStorageVO ps;
        if (msg.getVolumeUuid() != null) {
            String sql = "select ps from PrimaryStorageVO ps, VolumeVO vol where ps.uuid = vol.primaryStorageUuid" +
                    " and vol.uuid = :uuid";
            TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            q.setParameter("uuid", msg.getVolumeUuid());
            List<PrimaryStorageVO> pss = q.getResultList();
            ps = pss.isEmpty() ? null : pss.get(0);
        } else if (msg.getVolumeSnapshotUuid() != null) {
            String sql = "select ps from PrimaryStorageVO ps, VolumeSnapshotVO sp where ps.uuid = sp.primaryStorageUuid" +
                    " and sp.uuid = :uuid";
            TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            q.setParameter("uuid", msg.getVolumeSnapshotUuid());
            List<PrimaryStorageVO> pss = q.getResultList();
            ps = pss.isEmpty() ? null : pss.get(0);
        } else {
            throw new CloudRuntimeException("cannot be there");
        }

        if (ps == null) {
            throw new CloudRuntimeException("cannot find primary storage");
        }

        List<String> backupStorageTypes = getBackupStorageTypesByPrimaryStorageTypeFromMetrics(ps.getType());

        String sql = "select bs from BackupStorageVO bs, BackupStorageZoneRefVO ref, PrimaryStorageVO ps" +
                " where bs.uuid = ref.backupStorageUuid and ps.zoneUuid = ref.zoneUuid and ps.uuid = :psUuid" +
                " and bs.type in (:bsTypes) and bs.state = :bsState and bs.status = :bsStatus";

        TypedQuery<BackupStorageVO> q = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
        q.setParameter("psUuid", ps.getUuid());
        q.setParameter("bsTypes", backupStorageTypes);
        q.setParameter("bsState", BackupStorageState.Enabled);
        q.setParameter("bsStatus", BackupStorageStatus.Connected);

        APIGetCandidateBackupStorageForCreatingImageReply reply = new APIGetCandidateBackupStorageForCreatingImageReply();
        reply.setInventories(BackupStorageInventory.valueOf(q.getResultList()));
        bus.reply(msg, reply);
    }

    private void handle(RecalculateHostCapacityMsg msg) {
        final List<String> hostUuids = new ArrayList<>();
        if (msg.getHostUuid() != null) {
            hostUuids.add(msg.getHostUuid());
        } else {
            SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
            q.select(HostVO_.uuid);
            q.add(HostVO_.zoneUuid, Op.EQ, msg.getZoneUuid());
            hostUuids.addAll(q.listValue());
        }

        if (hostUuids.isEmpty()) {
            return;
        }

        class HostUsedCpuMem {
            String hostUuid;
            Long usedMemory;
            Long usedCpu;
        }

        List<HostUsedCpuMem> hostUsedCpuMemList = new Callable<List<HostUsedCpuMem>>() {
            @Override
            @Transactional(readOnly = true)
            public List<HostUsedCpuMem> call() {
                String sql = "select sum(vm.memorySize), vm.hostUuid, sum(vm.cpuNum)" +
                        " from VmInstanceVO vm" +
                        " where vm.hostUuid in (:hostUuids)" +
                        " and vm.state not in (:vmStates)" +
                        " group by vm.hostUuid";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("hostUuids", hostUuids);
                q.setParameter("vmStates", list(
                        VmInstanceState.Destroyed,
                        VmInstanceState.Created,
                        VmInstanceState.Destroying,
                        VmInstanceState.Stopped));
                List<Tuple> ts = q.getResultList();

                List<HostUsedCpuMem> ret = new ArrayList<>();
                for (Tuple t : ts) {
                    HostUsedCpuMem s = new HostUsedCpuMem();
                    s.hostUuid = t.get(1, String.class);

                    if (t.get(0, Long.class) == null) {
                        continue;
                    }

                    s.usedMemory = ratioMgr.calculateMemoryByRatio(s.hostUuid, t.get(0, Long.class));
                    s.usedCpu = t.get(2, Long.class);
                    ret.add(s);
                }
                return ret;
            }
        }.call();

        List<String> hostHasVms = CollectionUtils.transformToList(hostUsedCpuMemList, new Function<String, HostUsedCpuMem>() {
            @Override
            public String call(HostUsedCpuMem arg) {
                return arg.hostUuid;
            }
        });

        hostUuids.stream().filter(huuid -> !hostHasVms.contains(huuid)).forEach(huuid -> {
            HostUsedCpuMem s = new HostUsedCpuMem();
            s.hostUuid = huuid;
            hostUsedCpuMemList.add(s);
        });

        for (final HostUsedCpuMem s : hostUsedCpuMemList) {
            new HostCapacityUpdater(s.hostUuid).run(new HostCapacityUpdaterRunnable() {
                @Override
                public HostCapacityVO call(HostCapacityVO cap) {
                    long before = cap.getAvailableMemory();
                    long avail = s.usedMemory == null ? cap.getTotalMemory() : cap.getTotalMemory() - s.usedMemory;
                    cap.setAvailableMemory(avail);

                    long totalCpu = cpuRatioMgr.calculateHostCpuByRatio(s.hostUuid, cap.getCpuNum());
                    long totalCpuBefore = cap.getTotalCpu();
                    cap.setTotalCpu(totalCpu);

                    long beforeCpu = cap.getAvailableCpu();
                    long availCpu = s.usedCpu == null ? cap.getTotalCpu() : cap.getTotalCpu() - s.usedCpu;
                    cap.setAvailableCpu(availCpu);

                    logger.debug(String.format("re-calculated available capacity on the host[uuid:%s]:" +
                                    "\n[available memory] before: %s, now: %s" +
                                    "\n[total cpu] before: %s, now: %s" +
                                    "\n[available cpu] before: %s, now :%s",
                            s.hostUuid,
                            before, avail,
                            totalCpuBefore, totalCpu,
                            beforeCpu, availCpu));
                    return cap;
                }
            });
        }
    }

    private void handle(ReturnHostCapacityMsg msg) {
        returnComputeResourceCapacity(msg.getHostUuid(), msg.getCpuCapacity(), msg.getMemoryCapacity());
    }

    private void handle(ReportHostCapacityMessage msg) {
        long totalCpu = cpuRatioMgr.calculateHostCpuByRatio(msg.getHostUuid(), msg.getCpuNum());
        long availMem = msg.getTotalMemory() - msg.getUsedMemory();
        availMem = availMem > 0 ? availMem : 0;
        long availCpu = totalCpu - msg.getUsedCpu();
        availCpu = availCpu > 0 ? availCpu : 0;

        HostCapacityVO vo = dbf.findByUuid(msg.getHostUuid(), HostCapacityVO.class);
        if (vo == null) {
            vo = new HostCapacityVO();
            vo.setUuid(msg.getHostUuid());
            vo.setTotalCpu(totalCpu);
            vo.setAvailableCpu(availCpu);
            vo.setTotalMemory(msg.getTotalMemory());
            vo.setAvailableMemory(availMem);
            vo.setTotalPhysicalMemory(msg.getTotalMemory());
            vo.setAvailablePhysicalMemory(availMem);
            vo.setCpuNum(msg.getCpuNum());

            HostCapacityStruct s = new HostCapacityStruct();
            s.setCapacityVO(vo);
            s.setCpuNum(msg.getCpuNum());
            s.setTotalCpu(totalCpu);
            s.setTotalMemory(msg.getTotalMemory());
            s.setUsedCpu(msg.getUsedCpu());
            s.setUsedMemory(msg.getUsedMemory());
            s.setInit(true);
            for (ReportHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(ReportHostCapacityExtensionPoint.class)) {
                vo = ext.reportHostCapacity(s);
            }
            dbf.persist(vo);
        } else {
            vo.setCpuNum(msg.getCpuNum());
            vo.setTotalCpu(totalCpu);
            vo.setAvailableCpu(availCpu);
            vo.setTotalPhysicalMemory(msg.getTotalMemory());
            vo.setAvailablePhysicalMemory(availMem);
            vo.setTotalMemory(msg.getTotalMemory());

            HostCapacityStruct s = new HostCapacityStruct();
            s.setCapacityVO(vo);
            s.setTotalCpu(totalCpu);
            s.setTotalMemory(msg.getTotalMemory());
            s.setUsedCpu(msg.getUsedCpu());
            s.setUsedMemory(msg.getUsedMemory());
            s.setInit(false);
            for (ReportHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(ReportHostCapacityExtensionPoint.class)) {
                vo = ext.reportHostCapacity(s);
            }
            dbf.update(vo);
        }
    }

    private void handle(final AllocateHostMsg msg) {
        HostAllocatorSpec spec = HostAllocatorSpec.fromAllocationMsg(msg);
        spec.setBackupStoragePrimaryStorageMetrics(backupStoragePrimaryStorageMetrics);

        String allocatorStrategyType = null;
        for (HostAllocatorStrategyExtensionPoint ext : pluginRgty.getExtensionList(HostAllocatorStrategyExtensionPoint.class)) {
            allocatorStrategyType = ext.getHostAllocatorStrategyName(spec);
            if (allocatorStrategyType != null) {
                logger.debug(String.format("%s returns allocator strategy type[%s]", ext.getClass(), allocatorStrategyType));
                break;
            }
        }

        if (allocatorStrategyType == null) {
            allocatorStrategyType = msg.getAllocatorStrategy();
        }

        HostAllocatorStrategyFactory factory = getHostAllocatorStrategyFactory(HostAllocatorStrategyType.valueOf(allocatorStrategyType));
        HostAllocatorStrategy strategy = factory.getHostAllocatorStrategy();
        factory.marshalSpec(spec, msg);

        if (msg.isDryRun()) {
            final AllocateHostDryRunReply reply = new AllocateHostDryRunReply();
            strategy.dryRun(spec, new ReturnValueCompletion<List<HostInventory>>() {
                @Override
                public void success(List<HostInventory> returnValue) {
                    reply.setHosts(returnValue);
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        } else {
            final AllocateHostReply reply = new AllocateHostReply();
            strategy.allocate(spec, new ReturnValueCompletion<HostInventory>(msg) {
                @Override
                public void success(HostInventory returnValue) {
                    reply.setHost(returnValue);
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetCpuMemoryCapacityMsg) {
            handle((APIGetCpuMemoryCapacityMsg) msg);
        } else if (msg instanceof APIGetHostAllocatorStrategiesMsg) {
            handle((APIGetHostAllocatorStrategiesMsg) msg);
        } else if (msg instanceof APIGetCandidateBackupStorageForCreatingImageMsg) {
            handle((APIGetCandidateBackupStorageForCreatingImageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetHostAllocatorStrategiesMsg msg) {
        APIGetHostAllocatorStrategiesReply reply = new APIGetHostAllocatorStrategiesReply();
        reply.setHostAllocatorStrategies(HostAllocatorStrategyType.getAllExposedTypeNames());
        bus.reply(msg, reply);
    }


    private void handle(final APIGetCpuMemoryCapacityMsg msg) {
        APIGetCpuMemoryCapacityReply reply = new APIGetCpuMemoryCapacityReply();

        Tuple ret = new Callable<Tuple>() {
            @Override
            @Transactional(readOnly = true)
            public Tuple call() {
                if (msg.getHostUuids() != null && !msg.getHostUuids().isEmpty()) {
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory)" +
                            " from HostCapacityVO hc, HostVO host" +
                            " where hc.uuid in (:hostUuids)" +
                            " and hc.uuid = host.uuid" +
                            " and host.state = :hstate" +
                            " and host.status = :hstatus";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("hostUuids", msg.getHostUuids());
                    q.setParameter("hstate", HostState.Enabled);
                    q.setParameter("hstatus", HostStatus.Connected);
                    return q.getSingleResult();
                } else if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory)" +
                            " from HostCapacityVO hc, HostVO host" +
                            " where hc.uuid = host.uuid" +
                            " and host.clusterUuid in (:clusterUuids)" +
                            " and host.state = :hstate" +
                            " and host.status = :hstatus";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("clusterUuids", msg.getClusterUuids());
                    q.setParameter("hstate", HostState.Enabled);
                    q.setParameter("hstatus", HostStatus.Connected);
                    return q.getSingleResult();
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    String sql = "select sum(hc.totalCpu), sum(hc.availableCpu), sum(hc.availableMemory), sum(hc.totalMemory)" +
                            " from HostCapacityVO hc, HostVO host" +
                            " where hc.uuid = host.uuid" +
                            " and host.zoneUuid in (:zoneUuids)" +
                            " and host.state = :hstate" +
                            " and host.status = :hstatus";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("zoneUuids", msg.getZoneUuids());
                    q.setParameter("hstate", HostState.Enabled);
                    q.setParameter("hstatus", HostStatus.Connected);
                    return q.getSingleResult();
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        long totalCpu = ret.get(0, Long.class) == null ? 0 : ret.get(0, Long.class);
        long availCpu = ret.get(1, Long.class) == null ? 0 : ret.get(1, Long.class);
        long availMemory = ret.get(2, Long.class) == null ? 0 : ret.get(2, Long.class);
        long totalMemory = ret.get(3, Long.class) == null ? 0 : ret.get(3, Long.class);

        ReservedHostCapacity rc;
        if (msg.getHostUuids() != null && !msg.getHostUuids().isEmpty()) {
            rc = reserveMgr.getReservedHostCapacityByHosts(msg.getHostUuids());
        } else if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
            rc = reserveMgr.getReservedHostCapacityByClusters(msg.getClusterUuids());
        } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
            rc = reserveMgr.getReservedHostCapacityByZones(msg.getZoneUuids());
        } else {
            throw new CloudRuntimeException("should not be here");
        }

        availMemory = availMemory - rc.getReservedMemoryCapacity();
        availMemory = availMemory > 0 ? availMemory : 0;

        reply.setTotalCpu(totalCpu);
        reply.setTotalMemory(totalMemory);
        reply.setAvailableCpu(availCpu);
        reply.setAvailableMemory(availMemory);
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID);
    }

    private void populateHostAllocatorStrategyFactory() {
        for (HostAllocatorStrategyFactory ext : pluginRgty.getExtensionList(HostAllocatorStrategyFactory.class)) {
            HostAllocatorStrategyFactory old = factories.get(ext.getHostAllocatorStrategyType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HostAllocatorStrategyFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHostAllocatorStrategy()));
            }
            factories.put(ext.getHostAllocatorStrategyType().toString(), ext);
        }
    }

    @Override
    public boolean start() {
        populateHostAllocatorStrategyFactory();
        populatePrimaryStorageBackupStorageMetrics();
        return true;
    }

    private void populatePrimaryStorageBackupStorageMetrics() {
        for (Map.Entry<String, List<String>> e : backupStoragePrimaryStorageMetrics.entrySet()) {
            String bsType = e.getKey();
            List<String> psTypes = e.getValue();
            for (String psType : psTypes) {
                List<String> bsTypes = primaryStorageBackupStorageMetrics.get(psType);
                if (bsTypes == null) {
                    bsTypes = new ArrayList<>();
                    primaryStorageBackupStorageMetrics.put(psType, bsTypes);
                }
                bsTypes.add(bsType);
            }
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public HostAllocatorStrategyFactory getHostAllocatorStrategyFactory(HostAllocatorStrategyType type) {
        HostAllocatorStrategyFactory factory = factories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Unable to find HostAllocatorStrategyFactory with type[%s]", type));
        }

        return factory;
    }

    @Override
    public void returnComputeResourceCapacity(final String hostUuid, final long cpu, final long memory) {
        new HostCapacityUpdater(hostUuid).run(new HostCapacityUpdaterRunnable() {
            @Override
            public HostCapacityVO call(HostCapacityVO cap) {
                {
                    long availCpu = cap.getAvailableCpu() + cpu;
                    availCpu = availCpu > cap.getTotalCpu() ? cap.getTotalCpu() : availCpu;
                /*
                if (availCpu > cap.getTotalCpu()) {
                    throw new CloudRuntimeException(String.format("invalid cpu capcity of the host[uuid:%s], available cpu[%s]" +
                            " is larger than the total cpu[%s]", hostUuid, availCpu, cap.getTotalCpu()));
                }
                */

                    cap.setAvailableCpu(availCpu);
                }

                {
                    long deltaMemory = ratioMgr.calculateMemoryByRatio(hostUuid, memory);
                    long availMemory = cap.getAvailableMemory() + deltaMemory;
                    if (availMemory > cap.getTotalMemory()) {
                        throw new CloudRuntimeException(
                                String.format("invalid memory capacity of host[uuid:%s]," +
                                                " available memory[%s] is greater than total memory[%s]." +
                                                " Available Memory before is [%s], Delta Memory is [%s].",
                                        hostUuid,
                                        new DecimalFormat(",###").format(availMemory),
                                        new DecimalFormat(",###").format(cap.getTotalMemory()),
                                        new DecimalFormat(",###").format(cap.getAvailableMemory()),
                                        new DecimalFormat(",###").format(deltaMemory)
                                )
                        );
                    }

                    cap.setAvailableMemory(availMemory);
                }
                return cap;
            }
        });
    }

    @Override
    public Flow createVmAbnormalLifeCycleHandlingFlow(final VmAbnormalLifeCycleStruct struct) {
        return new Flow() {
            String __name__ = "allocate-host-capacity";

            VmAbnormalLifeCycleOperation operation = struct.getOperation();
            Runnable rollback;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (operation == VmAbnormalLifeCycleOperation.VmRunningOnTheHost) {
                    vmRunningOnHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmMigrateToAnotherHost) {
                    vmMigrateToAnotherHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromIntermediateState) {
                    vmRunningFromIntermediateState(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost) {
                    vmStoppedOnTheSameHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostChanged) {
                    vmRunningFromUnknownStateHostChanged(trigger);
                } else {
                    trigger.next();
                }
            }

            private void vmRunningFromUnknownStateHostChanged(FlowTrigger trigger) {
                // resync the capacity on the current host
                resyncHostCapacity();
                trigger.next();
            }

            private void resyncHostCapacity() {
                //TODO
            }

            private void vmStoppedOnTheSameHost(FlowTrigger trigger) {
                // return the capacity to the current host
                returnComputeCapacity(struct.getCurrentHostUuid());
                rollback = new Runnable() {
                    @Override
                    public void run() {
                        long cpu = struct.getVmInstance().getCpuNum();
                        new HostAllocatorChain().reserveCapacity(
                                struct.getCurrentHostUuid(), cpu, struct.getVmInstance().getMemorySize());
                    }
                };
                trigger.next();
            }

            private void returnComputeCapacity(String hostUuid) {
                ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
                msg.setCpuCapacity(struct.getVmInstance().getCpuNum());
                msg.setMemoryCapacity(struct.getVmInstance().getMemorySize());
                msg.setHostUuid(hostUuid);
                bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
                bus.send(msg);
            }

            private void vmRunningFromIntermediateState(FlowTrigger trigger) {
                // resync the capacity on the current host
                resyncHostCapacity();
                trigger.next();
            }

            private void vmMigrateToAnotherHost(FlowTrigger trigger) {
                // allocate the capacity on the current host
                // return the capacity to the original host
                try {
                    final long cpu = struct.getVmInstance().getCpuNum();
                    new HostAllocatorChain().reserveCapacity(
                            struct.getCurrentHostUuid(), cpu, struct.getVmInstance().getMemorySize());
                    returnComputeCapacity(struct.getOriginalHostUuid());

                    rollback = new Runnable() {
                        @Override
                        public void run() {
                            returnComputeCapacity(struct.getCurrentHostUuid());
                            new HostAllocatorChain().reserveCapacity(
                                    struct.getOriginalHostUuid(), cpu, struct.getVmInstance().getMemorySize());
                        }
                    };

                    trigger.next();
                } catch (UnableToReserveHostCapacityException e) {
                    trigger.fail(errf.stringToOperationError(e.getMessage()));
                }
            }

            private void vmRunningOnHost(FlowTrigger trigger) {
                // allocate capacity on the current host
                try {
                    long cpu = struct.getVmInstance().getCpuNum();
                    new HostAllocatorChain().reserveCapacity(
                            struct.getCurrentHostUuid(), cpu, struct.getVmInstance().getMemorySize());

                    rollback = new Runnable() {
                        @Override
                        public void run() {
                            returnComputeCapacity(struct.getCurrentHostUuid());
                        }
                    };

                    trigger.next();
                } catch (UnableToReserveHostCapacityException e) {
                    trigger.fail(errf.stringToOperationError(e.getMessage()));
                }
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (rollback != null) {
                    rollback.run();
                }

                trigger.rollback();
            }
        };
    }

    public void setBackupStoragePrimaryStorageMetrics(Map<String, List<String>> backupStoragePrimaryStorageMetrics) {
        this.backupStoragePrimaryStorageMetrics = backupStoragePrimaryStorageMetrics;
    }

    public Map<String, List<String>> getBackupStoragePrimaryStorageMetrics() {
        return backupStoragePrimaryStorageMetrics;
    }

    @Override
    public List<String> getPrimaryStorageTypesByBackupStorageTypeFromMetrics(String backupStorageType) {
        List<String> psTypes = backupStoragePrimaryStorageMetrics.get(backupStorageType);
        if (psTypes == null) {
            throw new CloudRuntimeException(String.format("cannot find supported primary storage types by the backup storage type[%s]", backupStorageType));
        }

        return psTypes;
    }

    @Override
    public List<String> getBackupStorageTypesByPrimaryStorageTypeFromMetrics(String psType) {
        List<String> bsTypes = primaryStorageBackupStorageMetrics.get(psType);
        if (bsTypes == null) {
            throw new CloudRuntimeException(String.format("cannot find supported backup storage types by the primary storage type[%s]", psType));
        }

        return bsTypes;
    }
}
