package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
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
import java.util.stream.Collectors;

import static org.zstack.header.host.HostConstant.SORTED_CLUSTERS;

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

        List<PsGroups> sortedPsGroup = getSortedPrimaryStorageGroups(data, spec);
        if (sortedPsGroup.isEmpty()) {
            allocate(trigger, spec);
            return;
        }

        List<String> localPsUuids = new ArrayList<>();
        List<String> nonLocalPsUuids = new ArrayList<>();
        List<String[]> psCombos = new ArrayList<>();

        for (PsGroups possiblePsUuids : sortedPsGroup) {
            // First priority：root local, data non-local
            // Second priority：root local, data local / root non-local, data non-local
            // Third priority：root non-local, data local
            List<String[]> psCombos1 = new ArrayList<>();
            List<String[]> psCombos2 = new ArrayList<>();
            List<String[]> psCombos3 = new ArrayList<>();

            List<String> availablePsUuids = new ArrayList<>();
            List<String> temporaryLocalPsUuids = new ArrayList<>();
            List<String> temporaryNonLocalPsUuids = new ArrayList<>();

            List<Tuple> availablePsTuples = Q.New(PrimaryStorageVO.class)
                    .select(PrimaryStorageVO_.uuid, PrimaryStorageVO_.type)
                    .in(PrimaryStorageVO_.uuid, possiblePsUuids.psList)
                    .eq(PrimaryStorageVO_.state, PrimaryStorageState.Enabled)
                    .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                    .listTuple();

            for (Tuple tuple : availablePsTuples) {
                String psUuid = (String) tuple.get(0);
                String psType = (String) tuple.get(1);
                if (psType.equals(PrimaryStorageConstants.LOCAL_STORAGE_TYPE)) {
                    temporaryLocalPsUuids.add(psUuid);
                } else {
                    temporaryNonLocalPsUuids.add(psUuid);
                }
            }

            List<String> sortedLocalPsUuids = sortPrimaryStorageByAvailableCapacity(temporaryLocalPsUuids);
            List<String> sortedNonLocalPsUuids = sortPrimaryStorageByAvailableCapacity(temporaryNonLocalPsUuids);

            localPsUuids.addAll(sortedLocalPsUuids);
            nonLocalPsUuids.addAll(sortedNonLocalPsUuids);

            availablePsUuids.addAll(sortedLocalPsUuids);
            availablePsUuids.addAll(sortedNonLocalPsUuids);
            if (availablePsUuids.isEmpty()) {
                continue;
            }

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

            psCombos.addAll(psCombos1);
            psCombos.addAll(psCombos2);
            psCombos.addAll(psCombos3);
        }

        boolean autoAllocateRootVolumePs = needAutoAllocateRootVolumePS(spec);
        boolean autoAllocateDataVolumePs = needAutoAllocateDataVolumePS(spec);
        List<ErrorCode> errorCodes = new ArrayList<>();

        if (autoAllocateRootVolumePs && autoAllocateDataVolumePs) {
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
                    if (errorCodes.size() == psCombos.size()) {
                        trigger.fail(errorCodes.get(0));
                        return;
                    }

                    trigger.next();
                }
            });
            return;
        }

        List<String> availablePsUuids = new ArrayList<>();
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

    private List<String> sortPrimaryStorageByAvailableCapacity(List<String> psUuids) {
        if (psUuids.isEmpty()) {
            return new ArrayList<>();
        }
        List<PrimaryStorageCapacityVO> primaryStorageCapacityVO = Q.New(PrimaryStorageCapacityVO.class).in(PrimaryStorageVO_.uuid, psUuids).list();
        return primaryStorageCapacityVO.stream()
                .sorted(Comparator.comparing(PrimaryStorageCapacityVO::getAvailableCapacity).reversed())
                .map(PrimaryStorageCapacityVO::getUuid).collect(Collectors.toList());
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

    static class PsGroups {
        List<String> psList;

        PsGroups(List<String> psList) {
            this.psList = psList;
        }
    }

    static class ClusterGroups {
        List<String> clusterList;

        ClusterGroups(List<String> clusterList) {
            this.clusterList = clusterList;
        }
    }

    private List<PsGroups> getSortedPrimaryStorageGroups(final Map data, VmInstanceSpec spec) {
        List<String> sortedClusters = (List<String>) data.get(SORTED_CLUSTERS);

        if (sortedClusters.isEmpty()) {
            return Collections.emptyList();
        }

        Map<PsGroups, ClusterGroups> psGroupsMapToClusterGroups = getPsGroupsMapToClusterGroups(sortedClusters, spec);

        List<PsGroups> psGroupsList = new ArrayList<>();
        List<ClusterGroups> clusterGroupsList = new ArrayList<>();
        for (Map.Entry<PsGroups, ClusterGroups> entry : psGroupsMapToClusterGroups.entrySet()) {
            psGroupsList.add(entry.getKey());
            clusterGroupsList.add(entry.getValue());
        }

        if (!isNeedAutoAllocatePrimaryStorage(psGroupsList)) {
            return Collections.emptyList();
        }

        List<PsGroups> sortedPsGroup = new ArrayList<>();
        sortedClusters.forEach(cluster -> clusterGroupsList.forEach(clusterGroups -> {
            if (clusterGroups.clusterList.contains(cluster)) {
                PsGroups psGroups = psGroupsList.get(clusterGroupsList.indexOf(clusterGroups));
                if (!sortedPsGroup.contains(psGroups)) {
                    sortedPsGroup.add(psGroups);
                }
            }
        }));

        return sortedPsGroup;
    }

    private Map<PsGroups, ClusterGroups> getPsGroupsMapToClusterGroups(List<String> sortedClusters, VmInstanceSpec spec) {
        String targetClusterUuid = getTargetCluster(spec);
        if (targetClusterUuid != null) {
            sortedClusters = Collections.singletonList(targetClusterUuid);
        }

        Map<String, PsGroups> clusterAccessiblePSs = new HashMap<>();
        sortedClusters.forEach(clusterUuid -> {
            PsGroups psGroups = getPrimaryStoragesFromClusters(clusterUuid);
            if (psGroups.psList.size() != 0) {
                clusterAccessiblePSs.put(clusterUuid, psGroups);
            }
        });

        Map<PsGroups, ClusterGroups> psGroupsMapToClusterGroups = new HashMap<>();
        clusterAccessiblePSs.forEach((key, value) -> {
            if (psGroupsMapToClusterGroups.get(value) == null) {
                psGroupsMapToClusterGroups.put(value, new ClusterGroups(new ArrayList<>(Collections.singleton(key))));
            } else {
                psGroupsMapToClusterGroups.get(value).clusterList.add(key);
            }
        });

        return psGroupsMapToClusterGroups;
    }

    private PsGroups getPrimaryStoragesFromClusters(String clusterUuid) {
        return new PsGroups(Q.New(PrimaryStorageClusterRefVO.class).select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid).listValues());
    }

    private boolean isNeedAutoAllocatePrimaryStorage(List<PsGroups> psGroupsList) {
        return psGroupsList.stream().map(this::getPrimaryStorageTypes).filter(this::filterRules).count() == psGroupsList.size();
    }

    private List<String> getPrimaryStorageTypes(PsGroups psGroups) {
        return Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).in(PrimaryStorageVO_.uuid, psGroups.psList).listValues();
    }

    private boolean filterRules(List<String> psTypes) {
        return psTypes.contains(PrimaryStorageConstants.LOCAL_STORAGE_TYPE);
    }
}
