package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.ReturnHostCapacityMsg;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;

import static org.zstack.core.Platform.operr;

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

        psAndcluster pc = getClusterGroup(trigger, data, spec);
        List<ErrorCode> errorCodesOut = new ArrayList<>();
        new While<>(pc.newps).each((possiblePsUuids, whileCompletion) -> {

            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            setFlowMarshaller(chain);
            chain.setName(String.format("dryrun-allocate-host-%s", spec.getVmInventory().getUuid()));
            chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
            chain.then(new ShareFlow() {
                @Override
                public void setup() {

                    flow(new NoRollbackFlow() {
                        String __name__ = "dryrun-allocate-host";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            List<ErrorCode> errorCodes = new ArrayList<>();

                            List<Tuple> availablePsTuples = Q.New(PrimaryStorageVO.class)
                                    .select(PrimaryStorageVO_.uuid, PrimaryStorageVO_.type)
                                    .in(PrimaryStorageVO_.uuid, possiblePsUuids)
                                    .eq(PrimaryStorageVO_.state, PrimaryStorageState.Enabled)
                                    .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                                    .listTuple();

                            //该集群下的主存储都不可用
                            if (availablePsTuples.isEmpty()) {
                                errorCodes.add(operr("no available ps"));
                                return;
                            }

                            List<String> availablePsUuids = new ArrayList<>();
                            List<String> localPsUuids = new ArrayList<>();
                            List<String> nonLocalPsUuids = new ArrayList<>();
                            for (Tuple tuple : availablePsTuples) {
                                String psUuid = (String) tuple.get(0);
                                String psType = (String) tuple.get(1);
                                availablePsUuids.add((String) tuple.get(0));

                                if (psType.equals(PrimaryStorageConstants.LOCAL_STORAGE_TYPE)) {
                                    localPsUuids.add(psUuid);
                                } else {
                                    nonLocalPsUuids.add(psUuid);
                                }
                            }

                            boolean autoAllocateRootVolumePs = needAutoAllocateRootVolumePS(spec);
                            boolean autoAllocateDataVolumePs = needAutoAllocateDataVolumePS(spec);

                            //根云盘和数据云盘都未指定ps
                            if (autoAllocateRootVolumePs && autoAllocateDataVolumePs) {
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
                                if (!psCombos1.isEmpty()){
                                    psCombos.addAll(psCombos1);
                                }
                                if (!psCombos2.isEmpty()){
                                    psCombos.addAll(psCombos2);
                                }
                                if (!psCombos3.isEmpty()){
                                    psCombos.addAll(psCombos3);
                                }

                                new While<>(psCombos).each((psCombo, whileCompletionInn1) -> {

                                    spec.setRequiredPrimaryStorageUuidForRootVolume(psCombo[0]);
                                    spec.setRequiredPrimaryStorageUuidForDataVolume(psCombo[1]);

                                    FlowChain chain = buildAllocateHostAndPrimaryStorageFlowChain(trigger, spec);
                                    chain.done(new FlowDoneHandler(whileCompletionInn1) {
                                        @Override
                                        public void handle(Map data) {
                                            whileCompletionInn1.allDone();
                                        }
                                    }).error(new FlowErrorHandler(whileCompletionInn1) {
                                        @Override
                                        public void handle(ErrorCode errCode, Map data) {
                                            spec.setRequiredPrimaryStorageUuidForRootVolume(null);
                                            spec.setRequiredPrimaryStorageUuidForDataVolume(null);
                                            errorCodes.add(errCode);
                                            whileCompletionInn1.done();
                                        }
                                    }).start();

                                }).run(new WhileDoneCompletion(trigger) {
                                    @Override
                                    public void done(ErrorCodeList errorCodeList) {
                                        if (errorCodes.size() == availablePsUuids.size()) {
                                            errorCodesOut.add(errorCodes.get(0));
                                        }
                                        whileCompletion.done();
                                        //trigger.next();
                                    }
                                });
                                return;
                            }

                            availablePsUuids.clear();
                            String noAssginps = null;
                            if (autoAllocateRootVolumePs) {
                                if (!possiblePsUuids.contains(spec.getRequiredPrimaryStorageUuidForRootVolume())) {
                                    return;
                                }
                                availablePsUuids.addAll(localPsUuids);
                                availablePsUuids.addAll(nonLocalPsUuids);
                                noAssginps = "root";
                            } else if (autoAllocateDataVolumePs) {
                                if (spec.getRequiredPrimaryStorageUuidForDataVolume() != null) {
                                    if (!possiblePsUuids.contains(spec.getRequiredPrimaryStorageUuidForDataVolume())) {
                                        return;
                                    }
                                    availablePsUuids.addAll(nonLocalPsUuids);
                                    availablePsUuids.addAll(localPsUuids);
                                    noAssginps = "data";
                                }
                            }

                            String finalNoAssginps = noAssginps;

                            new While<>(availablePsUuids).each((psUuid, whileCompletionInn2) -> {
                                if (autoAllocateRootVolumePs) {
                                    spec.setRequiredPrimaryStorageUuidForRootVolume(psUuid);
                                } else if (autoAllocateDataVolumePs) {
                                    spec.setRequiredPrimaryStorageUuidForDataVolume(psUuid);
                                }

                                FlowChain chain = buildAllocateHostAndPrimaryStorageFlowChain(trigger, spec);
                                chain.done(new FlowDoneHandler(whileCompletionInn2) {
                                    @Override
                                    public void handle(Map data) {
                                        whileCompletionInn2.allDone();
                                    }
                                }).error(new FlowErrorHandler(whileCompletionInn2) {
                                    @Override
                                    public void handle(ErrorCode errCode, Map data) {
                                        if (Objects.equals(finalNoAssginps, "root")) {
                                            spec.setRequiredPrimaryStorageUuidForRootVolume(null);
                                        } else if (Objects.equals(finalNoAssginps, "data")) {
                                            spec.setRequiredPrimaryStorageUuidForDataVolume(null);
                                        }
                                        errorCodes.add(errCode);
                                        whileCompletionInn2.done();
                                    }
                                }).start();

                            }).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    if (errorCodes.size() == availablePsUuids.size()) {
                                        errorCodesOut.add(errorCodes.get(0));
                                    }
                                    whileCompletion.done();
                                    //trigger.next();
                                }
                            });

                        }
                    });

                }

            }).done(new FlowDoneHandler(whileCompletion) {
                @Override
                public void handle(Map data) {
                    whileCompletion.allDone();
                }
            }).error(new FlowErrorHandler(whileCompletion) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    errorCodesOut.add(errCode);
                    whileCompletion.done();
                }
            }).start();

        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodesOut.size() == pc.newps.size()) {
                    trigger.fail(errorCodesOut.get(0));
                    return;
                }
                trigger.next();
            }
        });
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

    private psAndcluster getClusterGroup(final FlowTrigger trigger, final Map data, VmInstanceSpec spec) {
        List<String> possibleClusterUuids = getPossibleClusterUuids(spec);

        // 从创建参数中，获取可能的集群，然后查询出每个集群加载的主存储。放入map中，<String集群，list<主存储>>
        Map<String, List<String>> clusterAccessiblePS = new HashMap<>();
        for (String clusterUuid : possibleClusterUuids) {
            clusterAccessiblePS.put(clusterUuid, getPrimaryStorageUuidsFromCluster(clusterUuid));
        }

        // 根据主存储将集群分组，相同的主存储为一组。放入map中，<List<主存储>,List<集群>>
        Map<List<String>, List<String>> psAndClusterGroup = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : clusterAccessiblePS.entrySet()) {
            if (psAndClusterGroup.get(entry.getValue()) != null) {
                psAndClusterGroup.get(entry.getValue()).add(entry.getKey());
            } else {
                psAndClusterGroup.put(entry.getValue(), new ArrayList<String>(Arrays.asList(entry.getKey())));
            }
        }

        List<String> clusterInventories = (List<String>) data.get("clusters");
        Iterator<String> clusterInventoriesIte = clusterInventories.iterator();

        //从map中分别提取出，主存储和集群list
        List<ArrayList<String>> ps = new ArrayList(psAndClusterGroup.keySet());
        List<ArrayList<String>> cluster = new ArrayList(psAndClusterGroup.values());
        //新的排序主存储和集群
        List<ArrayList<String>> newps = new ArrayList();
        List<ArrayList<String>> newcluster = new ArrayList();
        //依据返回的集群，排序主存储和集群
        while (clusterInventoriesIte.hasNext()) {
            String clu = clusterInventoriesIte.next();
            for (ArrayList<String> ct : cluster) {
                if (ct.contains(clu)) {
                    if (!newcluster.contains(ct)) {
                        newcluster.add(ct);
                        newps.add(ps.get(cluster.indexOf(ct)));
                    }
                    break;
                }
            }
        }
        return new psAndcluster(newps, newcluster);
    }

    class psAndcluster {
        psAndcluster(List<ArrayList<String>> newps, List<ArrayList<String>> newcluster) {
            this.newps = newps;
            this.newcluster = newcluster;
        }

        List<ArrayList<String>> newps;
        List<ArrayList<String>> newcluster;
    }
}
