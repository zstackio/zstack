package org.zstack.externalStorage.primary.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.workflow.NopeFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.NodeHealthy;
import org.zstack.header.storage.addon.StorageHealthy;
import org.zstack.header.storage.addon.primary.BaseVolumeInfo;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.*;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class ExternalPrimaryStorageKvmFactory implements KVMHostConnectExtensionPoint, KVMPingAgentNoFailureExtensionPoint,
        KvmVmActiveVolumeSyncExtensionPoint, KVMStartVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ExternalPrimaryStorageKvmFactory.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;


    @Transactional(readOnly = true)
    private List<ExternalPrimaryStorageVO> findExternalPsByClusterUuid(String clusterUuid) {
        return SQL.New("select pri from ExternalPrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                        " where pri.uuid = ref.primaryStorageUuid" +
                        " and ref.clusterUuid = :cuuid", ExternalPrimaryStorageVO.class)
                .param("cuuid", clusterUuid)
                .list();
    }

    private Map<String, PrimaryStorageHostStatus> getHostStatus(List<ExternalPrimaryStorageVO> extPss) {
        return Q.New(PrimaryStorageHostRefVO.class)
                .select(PrimaryStorageHostRefVO_.hostUuid, PrimaryStorageHostRefVO_.status)
                .in(PrimaryStorageHostRefVO_.primaryStorageUuid,
                        extPss.stream().map(ExternalPrimaryStorageVO::getUuid).collect(Collectors.toList()))
                .listTuple().stream()
                .collect(Collectors.toMap(
                        t -> t.get(0, String.class),
                        t -> t.get(1, PrimaryStorageHostStatus.class),
                        (o, n) -> n
                ));
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        List<ExternalPrimaryStorageVO> extPss = findExternalPsByClusterUuid(context.getInventory().getClusterUuid());
        if (extPss.isEmpty()) {
            return new NopeFlow();
        }

        return new NoRollbackFlow() {
            final String __name__ = "external-primary-storage-kvm-host-connecting-flow";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                checkHostStatus(context.getInventory(), extPss, new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errList) {
                        data.put(KVMConstant.CONNECT_HOST_PRIMARYSTORAGE_ERROR, errList);
                        trigger.next();
                    }
                });

            }
        };
    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        List<ExternalPrimaryStorageVO> extPss = findExternalPsByClusterUuid(host.getClusterUuid());
        if (extPss.isEmpty()) {
            completion.done();
            return;
        }

        checkHostStatus(host, extPss, new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errList) {
                completion.done();
            }
        });
    }

    private void checkHostStatus(KVMHostInventory host, List<ExternalPrimaryStorageVO> extPss, WhileDoneCompletion completion) {

        Map<String, PrimaryStorageHostStatus> hostStatus = getHostStatus(extPss);
        new While<>(extPss).each((extPs, compl) -> {
            logger.debug(String.format("checking host status for external primary storage[uuid:%s, name:%s] on KVM host[uuid:%s, name:%s]",
                    extPs.getUuid(), extPs.getName(), host.getUuid(), host.getName()));
            extPsFactory.getControllerSvc(extPs.getUuid()).reportNodeHealthy(host, new ReturnValueCompletion<NodeHealthy>(compl) {
                @Override
                public void success(NodeHealthy returnValue) {
                    ErrorCode err = null;
                    PrimaryStorageHostStatus status;
                    // TODO add multi protocol support
                    if (returnValue.getHealthy().values().stream().allMatch(h -> h == StorageHealthy.Ok)) {
                        status = PrimaryStorageHostStatus.Connected;
                    } else {
                        status = PrimaryStorageHostStatus.Disconnected;
                        err = operr("external primary storage[uuid:%s, name:%s] returns unhealthy status: %s",
                                extPs.getUuid(), extPs.getName(), returnValue.getHealthy());
                        compl.addError(err);
                    }

                    if (hostStatus.get(extPs.getUuid()) != status) {
                        updateHostStatus(host.getUuid(), extPs.getUuid(), status, err, compl);
                    } else {
                        compl.done();
                    }
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.done();
                }

                private void updateHostStatus(String hostUuid, String psUuid, PrimaryStorageHostStatus status, ErrorCode reason, NoErrorCompletion completion) {
                    UpdatePrimaryStorageHostStatusMsg msg = new UpdatePrimaryStorageHostStatusMsg();
                    msg.setPrimaryStorageUuid(psUuid);
                    msg.setHostUuid(hostUuid);
                    msg.setStatus(status);
                    msg.setReason(reason);
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
                    bus.send(msg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            completion.done();
                        }
                    });
                }
            });
        }).run(completion);
    }

    @Override
    public List<String> getStoragePathsForVolumeSync(HostInventory host, PrimaryStorageInventory attachedPs) {
        if (!PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE.equals(attachedPs.getType())) {
            return null;
        }

        PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(attachedPs.getUuid());
        if (nodeSvc == null) {
            return null;
        }

        return nodeSvc.getActiveVolumesLocation(host);
    }

    @Override
    public void handleInactiveVolume(HostInventory host, Map<PrimaryStorageInventory, List<String>> inactiveVolumePaths, Completion completion) {
        if (inactiveVolumePaths.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(inactiveVolumePaths.entrySet()).all((entry, compl) -> {
            PrimaryStorageInventory ps = entry.getKey();
            List<String> paths = entry.getValue();

            PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(ps.getUuid());
            if (nodeSvc == null) {
                compl.done();
                return;
            }

            List<BaseVolumeInfo> infos = paths.stream()
                    .map(path -> nodeSvc.getActiveVolumeInfo(path, host, false))
                    .collect(Collectors.toList());
            if (infos.isEmpty()) {
                compl.done();
                return;
            }

            // TODO: move to pre-check
            List<String> vmInUseVolUuids = SQL.New("select vol.uuid from VolumeVO vol, VmInstanceVO vm" +
                            " where vol.uuid in :volUuids" +
                            " and vol.vmInstanceUuid = vm.uuid" +
                            " and (vm.state in (:vmStates) or vm.hostUuid = :huuid)", String.class)
                    .param("vmStates", Arrays.asList(VmInstanceState.Starting, VmInstanceState.Migrating))
                    .param("huuid", host.getUuid())
                    .param("volUuids", infos.stream().map(BaseVolumeInfo::getUuid).collect(Collectors.toList()))
                    .list();
            if (!vmInUseVolUuids.isEmpty()) {
                logger.debug(String.format("volumes[uuids:%s] are still in use by VMs, skip deactivating them",
                        vmInUseVolUuids));
            }

            infos.removeIf(info -> vmInUseVolUuids.contains(info.getUuid()));
            new While<>(infos).each((info, c) -> {
                if (info.getInstallPath() == null) {
                    VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, info.getUuid()).find();
                    if (volume == null) {
                        c.done();
                        return;
                    }

                    info.setInstallPath(volume.getInstallPath());
                    info.setProtocol(volume.getProtocol());
                }

                nodeSvc.deactivate(info.getInstallPath(), info.getProtocol(), host, new Completion(c) {
                    @Override
                    public void success() {
                        c.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        c.addError(errorCode);
                        c.done();
                    }
                });
            }).run(new WhileDoneCompletion(compl) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (!errorCodeList.getCauses().isEmpty()) {
                        compl.addError(errorCodeList.getCauses().get(0));
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        List<VolumeInventory> vols = getManagerExclusiveVolume(spec);

        for (VolumeInventory vol : vols) {
            PrimaryStorageNodeSvc nodeSvc = getNodeService(vol);
            if (nodeSvc == null) {
                continue;
            }

            nodeSvc.getActiveClients(vol.getInstallPath(), vol.getProtocol()).forEach(client -> {
                if (!client.getManagerIp().equals(host.getManagementIp()) && !client.isInBlacklist()) {
                    // TODO use async call
                    HostVO clientHost = Q.New(HostVO.class).eq(HostVO_.managementIp, client.getManagerIp()).find();
                    if (clientHost != null) {
                        logger.debug(String.format("because volume[uuid:%s, installPath:%s] is in use by other KVM " +
                                        "host[uuid:%s, ip:%s], but to start on host[uuid:%s, ip:%s], " +
                                        "add it to blacklist",
                                vol.getUuid(), vol.getInstallPath(),
                                clientHost.getUuid(), clientHost.getManagementIp(),
                                host.getUuid(), host.getManagementIp()));
                        nodeSvc.blacklist(vol.getInstallPath(), vol.getProtocol(), HostInventory.valueOf(clientHost), new NopeCompletion());;
                    }
                }
            });
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
    }

    private PrimaryStorageNodeSvc getNodeService(VolumeInventory volumeInventory) {
        String identity = volumeInventory.getInstallPath().split("://")[0];
        if (!extPsFactory.support(identity)) {
            return null;
        }

        return extPsFactory.getNodeSvc(volumeInventory.getPrimaryStorageUuid());
    }

    private List<VolumeInventory> getManagerExclusiveVolume(VmInstanceSpec spec) {
        List<VolumeInventory> vols = new ArrayList<>();
        vols.add(spec.getDestRootVolume());
        vols.addAll(spec.getDestDataVolumes());

        vols.removeIf(info -> {
            if (info.getInstallPath() == null || info.isShareable()) {
                return true;
            }
            String identity = info.getInstallPath().split("://")[0];
            return !extPsFactory.support(identity);
        });

        return vols;
    }

}
