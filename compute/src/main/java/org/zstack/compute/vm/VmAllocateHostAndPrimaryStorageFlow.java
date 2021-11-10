package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.*;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.taskProgress;

/**
 * Create by lining at 2020/08/17
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostAndPrimaryStorageFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateHostAndPrimaryStorageFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        // The creation parameter specifies the primary storage, no need to automatically allocate the primary storage
        if (!needAutoAllocatePS(spec)) {
            allocate(trigger, spec);
            return;
        }

        List<String> possibleClusterUuids = getPossibleClusterUuids(spec);
        List<String> possiblePsUuids = getPossiblePrimaryStorageUuids(spec);

        // 从创建参数中，获取可能的集群，然后查询出每个集群加载的主存储。放入map中，<String集群，list<主存储>>
        Map<String, List<String>> clusterPS = new HashMap<>();
        for (String clusterUuid : possibleClusterUuids) {
            clusterPS.put(clusterUuid, getPrimaryStorageUuidsFromCluster(clusterUuid));
        }

        // 根据主存储将集群分组，相同的主存储为一组。放入map中，<List<主存储>,List<集群>>
        Map<List<String>, List<String>> clusterGroup = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : clusterPS.entrySet()) {
            if (clusterGroup.get(entry.getValue()) != null) {
                clusterGroup.get(entry.getValue()).add(entry.getKey());
            } else {
                clusterGroup.put(entry.getValue(), new ArrayList<String>(Arrays.asList(entry.getKey())));
            }
        }

        //试运行，根据策略获取满足条件的可能物理机，这个物理机是有顺序的（满足策略，但是不一定能够成功）
        // 依据物理的顺序，对集群排序
        AllocateHostMsg amsg = this.prepareMsg(spec);
        bus.makeLocalServiceId(amsg, HostAllocatorConstant.SERVICE_ID);
        bus.send(amsg, new CloudBusCallBack(amsg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    areply.setError(reply.getError());
                } else {
                    AllocateHostDryRunReply re = reply.castReply();

                    if (!re.getHosts().isEmpty()) {
                        areply.setHosts(re.getHosts());

                        List<String> clusterUuids = re.getHosts().stream().
                                map(HostInventory::getClusterUuid).collect(Collectors.toList());
                        areply.setClusters(ClusterInventory.valueOf(dbf.listByPrimaryKeys(clusterUuids, ClusterVO.class)));

                        List<String> zoneUuids = re.getHosts().stream().
                                map(HostInventory::getZoneUuid).collect(Collectors.toList());
                        areply.setZones(ZoneInventory.valueOf(dbf.listByPrimaryKeys(zoneUuids, ZoneVO.class)));
                    } else {
                        areply.setHosts(new ArrayList<>());
                        areply.setClusters(new ArrayList<>());
                        areply.setZones(new ArrayList<>());
                    }
                }

                bus.reply(msg, areply);
            }
        });

        bus.makeLocalServiceId(amsg, HostAllocatorConstant.SERVICE_ID);
        bus.send(amsg, new CloudBusCallBack(amsg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                AllocateHostDryRunReply r = reply.castReply();

            }
        });

        // 限制可能的主存储和可能的集群。使用分组中的集群和主存储进行分配。
        for (Map.Entry<List<String>, List<String>> entry : clusterGroup.entrySet()) {
            possibleClusterUuids = entry.getValue();
            possiblePsUuids = entry.getKey();

            // Multiple clusters, and each cluster has a different primary storage
            // Do not automatically allocate the primary storage, specifying primary storage impacts cluster selection
            if (possibleClusterUuids.size() > 1) {
                boolean clusterWithSamePs = true;
                for (String clusterUuid : possibleClusterUuids) {
                    List<String> psUuids = Q.New(PrimaryStorageClusterRefVO.class)
                            .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                            .eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid)
                            .listValues();
                    if (!psUuids.containsAll(possiblePsUuids)) {
                        clusterWithSamePs = false;
                        break;
                    }
                }

                if (!clusterWithSamePs) {
                    allocate(trigger, spec);
                    return;
                }
            }

            // Not local + non-local，no need to automatically allocate the primary storage
            if (!isMixPrimaryStorage(spec)) {
                allocate(trigger, spec);
                return;
            }

            List<Tuple> availablePsTuples = Q.New(PrimaryStorageVO.class)
                    .select(PrimaryStorageVO_.uuid, PrimaryStorageVO_.type)
                    .in(PrimaryStorageVO_.uuid, possiblePsUuids)
                    .eq(PrimaryStorageVO_.state, PrimaryStorageState.Enabled)
                    .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                    .listTuple();

            List<String> availablePsUuids = new ArrayList<>();
            List<String> localPsUuids = new ArrayList<>();
            List<String> nonLocalPsUuids = new ArrayList<>();
            for (Tuple tuple : availablePsTuples) {
                String psUuid = (String)tuple.get(0);
                String psType = (String)tuple.get(1);
                availablePsUuids.add((String)tuple.get(0));

                if (psType.equals(PrimaryStorageConstants.LOCAL_STORAGE_TYPE)) {
                    localPsUuids.add(psUuid);
                } else {
                    nonLocalPsUuids.add(psUuid);
                }
            }

            if (availablePsUuids.isEmpty()) {
                allocate(trigger, spec);
                return;
            }

            boolean autoAllocateRootVolumePs = needAutoAllocateRootVolumePS(spec);
            boolean autoAllocateDataVolumePs = needAutoAllocateDataVolumePS(spec);
            List<ErrorCode> errorCodes = new ArrayList<>();

            if (autoAllocateRootVolumePs && autoAllocateDataVolumePs) {
                // First priority：root local, data non-local
                // Second priority：root local, data local / root non-local, data non-local
                // Third priority：root non-local, data local
                List<String[]> psCombos1 = new ArrayList<>();
                List<String[]> psCombos2 = new ArrayList<>();
                List<String[]> psCombos3 = new ArrayList<>();

                for (String rootVolumePsUuid : availablePsUuids) {
                    for (String dataVolumePsUuid : availablePsUuids) {
                        String[] combo = {rootVolumePsUuid, dataVolumePsUuid};

                        if (localPsUuids.contains(rootVolumePsUuid) && nonLocalPsUuids.contains(dataVolumePsUuid)) {
                            psCombos1.add(combo);
                        } else if (nonLocalPsUuids.contains(rootVolumePsUuid) && localPsUuids.contains(dataVolumePsUuid)) {
                            psCombos3.add(combo);
                        } else {
                            psCombos2.add(combo);
                        }
                    }
                }

                List<String[]> psCombos = new ArrayList<>();
                psCombos.addAll(psCombos1);
                psCombos.addAll(psCombos2);
                psCombos.addAll(psCombos3);

                new While<>(psCombos).each((psCombo, whileCompletion) -> {
                    spec.setRequiredPrimaryStorageUuidForRootVolume(psCombo[0]);
                    spec.setRequiredPrimaryStorageUuidForDataVolume(psCombo[1]);

                    FlowChain chain = buildAllocateHostAndPrimaryStorageFlowChain(trigger, spec);
                    chain.done(new FlowDoneHandler(whileCompletion) {
                        @Override
                        public void handle(Map data) {
                            whileCompletion.allDone();
                        }
                    }).error(new FlowErrorHandler(whileCompletion) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            errorCodes.add(errCode);
                            whileCompletion.done();
                        }
                    }).start();
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodes.size() == availablePsUuids.size()) {
                            trigger.fail(errorCodes.get(0));
                            return;
                        }

                        trigger.next();
                    }
                });
                return;
            }

            availablePsUuids.clear();
            if (autoAllocateRootVolumePs) {
                availablePsUuids.addAll(localPsUuids);
                availablePsUuids.addAll(nonLocalPsUuids);
            } else {
                availablePsUuids.addAll(nonLocalPsUuids);
                availablePsUuids.addAll(localPsUuids);
            }

            new While<>(availablePsUuids).each((psUuid, whileCompletion) -> {
                if (autoAllocateRootVolumePs) {
                    spec.setRequiredPrimaryStorageUuidForRootVolume(psUuid);
                } else {
                    spec.setRequiredPrimaryStorageUuidForDataVolume(psUuid);
                }

                FlowChain chain = buildAllocateHostAndPrimaryStorageFlowChain(trigger, spec);
                chain.done(new FlowDoneHandler(whileCompletion) {
                    @Override
                    public void handle(Map data) {
                        whileCompletion.allDone();
                    }
                }).error(new FlowErrorHandler(whileCompletion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        errorCodes.add(errCode);
                        whileCompletion.done();
                    }
                }).start();
            }).run(new WhileDoneCompletion(trigger) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (errorCodes.size() == availablePsUuids.size()) {
                        trigger.fail(errorCodes.get(0));
                        return;
                    }

                    trigger.next();
                }
            });
        }
    }

    private long getTotalDataDiskSize(VmInstanceSpec spec) {
        long size = 0;
        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            size += dinv.getDiskSize();
        }
        return size;
    }

    private AllocateHostMsg prepareMsg(VmInstanceSpec spec) {
        DesignatedAllocateHostMsg msg = new DesignatedAllocateHostMsg();

        List<DiskOfferingInventory> diskOfferings = new ArrayList<>();
        ImageInventory image = spec.getImageSpec().getInventory();
        long diskSize;
        if (image.getMediaType() != null && image.getMediaType().equals(ImageConstant.ImageMediaType.ISO.toString())) {
            DiskOfferingVO dvo = dbf.findByUuid(spec.getRootDiskOffering().getUuid(), DiskOfferingVO.class);
            diskSize = dvo.getDiskSize();
            diskOfferings.add(DiskOfferingInventory.valueOf(dvo));
        } else {
            diskSize = image.getSize();
        }
        diskSize += getTotalDataDiskSize(spec);
        diskOfferings.addAll(spec.getDataDiskOfferings());
        msg.setSoftAvoidHostUuids(spec.getSoftAvoidHostUuids());
        msg.setAvoidHostUuids(spec.getAvoidHostUuids());
        msg.setDiskOfferings(diskOfferings);
        msg.setDiskSize(diskSize);
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        List<L3NetworkInventory> l3Invs = VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks());
        msg.setL3NetworkUuids(CollectionUtils.transformToList(l3Invs,
                new Function<String, L3NetworkInventory>() {
                    @Override
                    public String call(L3NetworkInventory arg) {
                        return arg.getUuid();
                    }
                }));
        msg.setImage(image);
        msg.setVmOperation(spec.getCurrentVmOperation().toString());

        if (spec.getVmInventory().getZoneUuid() != null) {
            msg.setZoneUuid(spec.getVmInventory().getZoneUuid());
        }
        if (spec.getVmInventory().getClusterUuid() != null) {
            msg.setClusterUuid(spec.getVmInventory().getClusterUuid());
        }
        msg.setHostUuid(spec.getRequiredHostUuid());
        if (spec.getHostAllocatorStrategy() != null) {
            msg.setAllocatorStrategy(spec.getHostAllocatorStrategy());
        } else {
            msg.setAllocatorStrategy(spec.getVmInventory().getAllocatorStrategy());
        }
        if (spec.getRequiredPrimaryStorageUuidForRootVolume() != null) {
            msg.addRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
        }
        if (spec.getRequiredPrimaryStorageUuidForDataVolume() != null) {
            msg.addRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());
        }
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        msg.setVmInstance(spec.getVmInventory());

        if (spec.getImageSpec() != null && spec.getImageSpec().getSelectedBackupStorage() != null) {
            msg.setRequiredBackupStorageUuid(spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
        }

        msg.setListAllHostsGroupByCluster(true);
        return msg;
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        FlowChain rollbackChain = FlowChainBuilder.newShareFlowChain();
        rollbackChain.setName(String.format("rollback-allocate-host-and-ps-for-vm-%s", spec.getVmInventory().getUuid()));
        rollbackChain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        rollbackChain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                        for (VmInstanceSpec.VolumeSpec vspec : spec.getVolumeSpecs()) {
                            if (vspec.isVolumeCreated()) {
                                // don't return capacity as it has been returned when the volume is deleted
                                continue;
                            }

                            IncreasePrimaryStorageCapacityMsg msg = new IncreasePrimaryStorageCapacityMsg();
                            msg.setDiskSize(vspec.getSize());
                            msg.setPrimaryStorageUuid(vspec.getPrimaryStorageInventory().getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vspec.getPrimaryStorageInventory().getUuid());
                            bus.send(msg);
                        }
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                        HostInventory host = spec.getDestHost();

                        // if ChangeImage, then no need to ReturnHostCapacity, and resume vm info
                        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.ChangeImage) {
                            VmInstanceVO vmvo = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
                            vmvo.setClusterUuid(spec.getVmInventory().getClusterUuid());
                            vmvo.setLastHostUuid(spec.getVmInventory().getLastHostUuid());
                            vmvo.setHypervisorType(spec.getVmInventory().getHypervisorType());
                            dbf.update(vmvo);
                        } else if (host != null) {
                            ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
                            msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
                            msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
                            msg.setHostUuid(host.getUuid());
                            msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
                            bus.send(msg);
                        }

                        extEmitter.cleanUpAfterVmFailedToStart(spec.getVmInventory());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map data) {
                        chain.rollback();
                    }
                });

                error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        chain.rollback();
                    }
                });
            }
        }).start();
    }

    private void setFlowMarshaller(FlowChain chain) {
        chain.setFlowMarshaller(new FlowMarshaller() {
            @Override
            public Flow marshalTheNextFlow(String previousFlowClassName, String nextFlowClassName, FlowChain chain, Map data) {
                Flow nflow = null;
                for (MarshalVmOperationFlowExtensionPoint mext : pluginRgty.getExtensionList(MarshalVmOperationFlowExtensionPoint.class)) {
                    VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    nflow = mext.marshalVmOperationFlow(previousFlowClassName, nextFlowClassName, chain, spec);
                    if (nflow != null) {
                        logger.debug(String.format("a VM[uuid: %s, operation: %s] operation flow[%s] is changed to the flow[%s] by %s",
                                spec.getVmInventory().getName(), spec.getCurrentVmOperation(), nextFlowClassName, nflow.getClass(), mext.getClass()));
                        break;
                    }
                }

                return nflow;
            }
        });
    }

    // local + non-local
    private boolean isMixPrimaryStorage(VmInstanceSpec spec) {
        List<String> psUuids = getPossiblePrimaryStorageUuids(spec);
        if (psUuids.size() < 2) {
            return false;
        }

        List<String> psTypes = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .in(PrimaryStorageVO_.uuid, psUuids)
                .groupBy(PrimaryStorageVO_.type)
                .listValues();
        return !(!psTypes.contains(PrimaryStorageConstants.LOCAL_STORAGE_TYPE) || psTypes.size() <= 1);
    }

    private String getTargetCluster(VmInstanceSpec spec) {
        VmInstanceInventory vm = spec.getVmInventory();

        String clusterUuid = vm.getClusterUuid() != null ? vm.getClusterUuid() : spec.getRequiredClusterUuid();
        if (clusterUuid != null) {
            return clusterUuid;
        }

        String hostUuid = spec.getRequiredHostUuid();
        String l3Uuid = vm.getDefaultL3NetworkUuid();
        String zoneUuid = vm.getZoneUuid();
        String rootVolumePsUuid = spec.getRequiredPrimaryStorageUuidForRootVolume();
        String dataVolumePsUuid = spec.getRequiredPrimaryStorageUuidForDataVolume();

        if (hostUuid != null) {
            clusterUuid = Q.New(HostVO.class)
                    .select(HostVO_.clusterUuid)
                    .eq(HostVO_.uuid, hostUuid)
                    .findValue();
            return clusterUuid;
        }

        if (rootVolumePsUuid != null) {
            List<String> clusters = Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, rootVolumePsUuid)
                    .listValues();
            if (clusters.size() == 1) {
                return clusters.get(0);
            }
        }

        if (dataVolumePsUuid != null) {
            List<String> clusters = Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, dataVolumePsUuid)
                    .listValues();
            if (clusters.size() == 1) {
                return clusters.get(0);
            }
        }

        if (l3Uuid != null) {
            String l2Uuid = Q.New(L3NetworkVO.class)
                    .select(L3NetworkVO_.l2NetworkUuid)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .findValue();
            List<String> clusters = Q.New(L2NetworkClusterRefVO.class)
                    .select(L2NetworkClusterRefVO_.clusterUuid)
                    .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid)
                    .listValues();
            if (clusters.size() == 1) {
                return clusters.get(0);
            }
        }

        if (zoneUuid != null) {
            List<String> clusters = Q.New(ClusterVO.class)
                    .select(ClusterVO_.uuid)
                    .eq(ClusterVO_.zoneUuid, zoneUuid)
                    .listValues();
            if (clusters.size() == 1) {
                return clusters.get(0);
            }
        }

        return null;
    }

    private List<String> getPossibleClusterUuids(VmInstanceSpec spec) {
        String clusterUuid = getTargetCluster(spec);
        if (clusterUuid != null) {
            return Collections.singletonList(clusterUuid);
        }

        VmInstanceInventory vm = spec.getVmInventory();
        String l3Uuid = vm.getDefaultL3NetworkUuid();
        String zoneUuid = vm.getZoneUuid();
        String rootVolumePsUuid = spec.getRequiredPrimaryStorageUuidForRootVolume();
        String dataVolumePsUuid = spec.getRequiredPrimaryStorageUuidForDataVolume();

        if (rootVolumePsUuid != null) {
            return Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, rootVolumePsUuid)
                    .listValues();
        }

        if (dataVolumePsUuid != null) {
            return Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, dataVolumePsUuid)
                    .listValues();
        }

        if (l3Uuid != null) {
            String l2Uuid = Q.New(L3NetworkVO.class)
                    .select(L3NetworkVO_.l2NetworkUuid)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .findValue();
            return Q.New(L2NetworkClusterRefVO.class)
                    .select(L2NetworkClusterRefVO_.clusterUuid)
                    .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid)
                    .listValues();
        }

        if (zoneUuid != null) {
            return Q.New(ClusterVO.class)
                    .select(ClusterVO_.uuid)
                    .eq(ClusterVO_.zoneUuid, zoneUuid)
                    .listValues();
        }

        return Collections.emptyList();
    }

    private List<String> getPossiblePrimaryStorageUuids(VmInstanceSpec spec) {
        List<String> clusterUuids = getPossibleClusterUuids(spec);
        if (clusterUuids.isEmpty()) {
            return Collections.emptyList();
        }

        return SQL.New("select distinct(t0.uuid) from PrimaryStorageVO t0, PrimaryStorageClusterRefVO t1" +
                        " where t0.uuid = t1.primaryStorageUuid" +
                        " and t1.clusterUuid in (:clusterUuids)", String.class)
                .param("clusterUuids", clusterUuids)
                .list();
    }

    private boolean needAutoAllocatePS(VmInstanceSpec spec) {
        boolean autoAllocateRootVolumePs = needAutoAllocateRootVolumePS(spec);
        boolean autoAllocateDataVolumePs = needAutoAllocateDataVolumePS(spec);
        return  autoAllocateRootVolumePs || autoAllocateDataVolumePs;
    }

    private boolean needAutoAllocateRootVolumePS(VmInstanceSpec spec) {
        return spec.getRequiredPrimaryStorageUuidForRootVolume() == null;
    }

    private boolean needAutoAllocateDataVolumePS(VmInstanceSpec spec) {
        if (spec.getRequiredPrimaryStorageUuidForDataVolume() == null) {
            if (spec.getDataDiskOfferings() != null &&
                    spec.getDataDiskOfferings().size() > 0) {
                return true;
            }
        }

        return false;
    }

    private FlowChain buildAllocateHostAndPrimaryStorageFlowChain(final FlowTrigger trigger, VmInstanceSpec spec) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("allocate-host-and-ps-for-vm-%s", spec.getVmInventory().getUuid()));
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new VmAllocateHostFlow());

                flow(new VmAllocatePrimaryStorageFlow());
            }
        });
        setFlowMarshaller(chain);

        return chain;
    }

    private void allocate(final FlowTrigger trigger, VmInstanceSpec spec) {
        FlowChain chain = buildAllocateHostAndPrimaryStorageFlowChain(trigger, spec);
        chain.done(new FlowDoneHandler(trigger) {
            @Override
            public void handle(Map data) {
                trigger.next();
            }
        }).error(new FlowErrorHandler(trigger) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                trigger.fail(errCode);
            }
        });
        chain.start();
    }

    private List<String> getPrimaryStorageUuidsFromCluster(String clusterUuid) {
        return Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid).listValues();
    }
}
