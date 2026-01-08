package org.zstack.cbd.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.cbd.kvm.KvmCbdCommands.AgentRsp;
import org.zstack.cbd.kvm.KvmCbdCommands.KvmSetupSelfFencerCmd;
import org.zstack.cbd.kvm.KvmCbdCommands.KvmCancelSelfFencerCmd;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.kvm.*;
import org.zstack.storage.addon.primary.ExternalHostIdGetter;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * @author Xingwei Yu
 * @date 2024/4/9 16:22
 */

@FunctionalInterface
interface PathSetter<T> {
    void setPath(T target, String path);
}

public class KvmCbdNodeServer implements Component, KvmSetupSelfFencerExtensionPoint, KVMStartVmExtensionPoint,
        KVMConvertVolumeExtensionPoint, KVMDetachVolumeExtensionPoint, KVMAttachVolumeExtensionPoint, KVMPreAttachIsoExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmCbdNodeServer.class);

    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String kvmSetupSelfFencerStorageType() {
        return VolumeProtocol.CBD.toString();
    }

    @Override
    public void kvmSetupSelfFencer(KvmSetupSelfFencerParam param, Completion completion) {
        PrimaryStorageNodeSvc nodeSvc = extPsFactory.getNodeSvc(param.getPrimaryStorage().getUuid());
        HostInventory host = HostInventory.valueOf(dbf.findByUuid(param.getHostUuid(), HostVO.class));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("setup-self-fencer-for-external-primary-storage-%s-on-kvm-%s", param.getPrimaryStorage().getUuid(), host.getUuid()));
        chain.then(new ShareFlow() {
            HeartbeatVolumeTopology heartbeatVolumeTopology;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    final String __name__ = "deploy-client";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        nodeSvc.deployClient(host, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    final String __name__ = "activate-cbd-heartbeat-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        nodeSvc.activateHeartbeatVolume(host, new ReturnValueCompletion<HeartbeatVolumeTopology>(trigger) {
                            @Override
                            public void success(HeartbeatVolumeTopology topology) {
                                heartbeatVolumeTopology = topology;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    final String __name__ = "setup-cbd-self-fencer-on-kvm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExternalPrimaryStorageHostRefVO ref = Q.New(ExternalPrimaryStorageHostRefVO.class)
                                .eq(ExternalPrimaryStorageHostRefVO_.hostUuid, param.getHostUuid())
                                .eq(ExternalPrimaryStorageHostRefVO_.primaryStorageUuid, param.getPrimaryStorage().getUuid())
                                .find();
                        if (ref == null || ref.getHostId() == 0) {
                            logger.warn(String.format("not found hostId for hostUuid[%s] and primaryStorageUuid[%s]", param.getHostUuid(), param.getPrimaryStorage().getUuid()));
                            ref = new ExternalHostIdGetter(999).getOrAllocateHostIdRef(param.getHostUuid(), param.getPrimaryStorage().getUuid());
                        }

                        KvmSetupSelfFencerCmd cmd = new KvmSetupSelfFencerCmd();
                        cmd.interval = param.getInterval();
                        cmd.maxAttempts = param.getMaxAttempts();
                        cmd.heartbeatPathByCoveringPaths = heartbeatVolumeTopology
                                .getHeartbeatVolumeByCoveringPaths().entrySet().stream().collect(Collectors.toMap(
                                        Map.Entry::getKey, it -> it.getValue().getInstallPath()
                                ));
                        cmd.storageCheckerTimeout = param.getStorageCheckerTimeout();
                        cmd.heartbeatRequiredSpace = heartbeatVolumeTopology.getHeartbeatVolumeByCoveringPaths()
                                .values().iterator().next().getHeartbeatRequiredSpace();
                        cmd.hostUuid = param.getHostUuid();
                        cmd.hostId = ref.getHostId();
                        cmd.strategy = param.getStrategy();
                        cmd.uuid = param.getPrimaryStorage().getUuid();
                        cmd.fencers = param.getFencers();
                        httpCall(KvmCbdCommands.SETUP_CBD_SELF_FENCER_PATH, param.getHostUuid(), cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
                            @Override
                            public void success(AgentRsp returnValue) {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void kvmCancelSelfFencer(KvmCancelSelfFencerParam param, Completion completion) {
        KvmCancelSelfFencerCmd cmd = new KvmCancelSelfFencerCmd();
        cmd.uuid = param.getPrimaryStorage().getUuid();
        httpCall(KvmCbdCommands.CANCEL_CBD_SELF_FENCER_PATH, param.getHostUuid(), cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(completion) {
            @Override
            public void success(AgentRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    protected <T extends AgentRsp> void httpCall(String path, final String hostUuid, KVMAgentCommands.AgentCommand cmd, final Class<T> respType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, respType, completion);
    }

    protected <T extends AgentRsp> void httpCall(String path, final String hostUuid, KVMAgentCommands.AgentCommand cmd, boolean noCheckStatus, final Class<T> respType, final ReturnValueCompletion<T> completion) {
        DebugUtils.Assert(hostUuid != null, "Host must be set here");
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                KVMHostAsyncHttpCallReply rep = reply.castReply();
                final T rsp = rep.toResponse(respType);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }
                completion.success(rsp);
            }
        });
    }

    private String convertPathIfNeeded(BaseVolumeInfo volumeInfo, HostInventory host){
        if (!VolumeProtocol.CBD.name().equals(volumeInfo.getProtocol())){
            return volumeInfo.getInstallPath();
        }

        PrimaryStorageNodeSvc nodeSvc = getNodeService(volumeInfo);
        if (nodeSvc == null) {
            return volumeInfo.getInstallPath();
        }

        return nodeSvc.getActivePath(volumeInfo, host, false);
    }

    private <T> void convertAndSetPathIfNeeded(BaseVolumeInfo volumeInfo, HostInventory host, T target, PathSetter<T> setter) {
        String newInstallPath = convertPathIfNeeded(volumeInfo, host);
        setter.setPath(target, newInstallPath);
    }


    private PrimaryStorageNodeSvc getNodeService(BaseVolumeInfo volumeInfo) {
        String identity = volumeInfo.getInstallPath().split("://")[0];
        if (!extPsFactory.support(identity)) {
            return null;
        }

        return extPsFactory.getNodeSvc(volumeInfo.getPrimaryStorageUuid());
    }

    private VolumeTO convertVolumeIfNeeded(VolumeInventory volumeInventory, HostInventory host, VolumeTO volumeTO) {
        BaseVolumeInfo volumeInfo = BaseVolumeInfo.valueOf(volumeInventory);
        convertAndSetPathIfNeeded(volumeInfo, host, volumeTO, VolumeTO::setInstallPath);

        return volumeTO;
    }

    private KVMAgentCommands.CdRomTO convertCdRomIfNeeded(VmInstanceSpec.CdRomSpec cdRomSpec, HostInventory host, KVMAgentCommands.CdRomTO cdRomTO) {
        BaseVolumeInfo cdRomInfo = BaseVolumeInfo.valueOf(cdRomSpec);
        convertAndSetPathIfNeeded(cdRomInfo, host, cdRomTO, KVMAgentCommands.CdRomTO::setPath);

        return cdRomTO;
    }

    private KVMAgentCommands.IsoTO convertIsoIfNeeded( VmInstanceSpec.IsoSpec isoSpec ,HostInventory host, KVMAgentCommands.IsoTO isoTO) {
        BaseVolumeInfo isoInfo = BaseVolumeInfo.valueOf(isoSpec);
        convertAndSetPathIfNeeded(isoInfo, host, isoTO, KVMAgentCommands.IsoTO::setPath);

        return isoTO;
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        cmd.setVolume(convertVolumeIfNeeded(volume, host, cmd.getVolume()));
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {
    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {

    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return convertVolumeIfNeeded(inventory, host, to);
    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
        cmd.setVolume(convertVolumeIfNeeded(volume, host, cmd.getVolume()));
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {

    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        cmd.setRootVolume(convertVolumeIfNeeded(spec.getDestRootVolume(), host, cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dtos.add(convertVolumeIfNeeded(vol, host, to));
                    break;
                }
            }
        }

        cmd.setDataVolumes(dtos);

        List<KVMAgentCommands.CdRomTO> ctos = new ArrayList<>();
        for (KVMAgentCommands.CdRomTO cto : cmd.getCdRoms()){
            for (VmInstanceSpec.CdRomSpec cdRom : spec.getCdRomSpecs()){
                if (cdRom.getUuid().equals(cto.getResourceUuid())){
                    ctos.add(convertCdRomIfNeeded(cdRom, host, cto));
                    break;
                }
            }
        }
        cmd.setCdRoms(ctos);

    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    public void preAttachIsoExtensionPoint(KVMHostInventory host, KVMAgentCommands.AttachIsoCmd cmd) {
        KVMAgentCommands.IsoTO isoTO = cmd.getIso();
        VmInstanceSpec.IsoSpec isoSpec = new VmInstanceSpec.IsoSpec();
        isoSpec.setDeviceId(isoTO.getDeviceId());
        isoSpec.setImageUuid(isoTO.getImageUuid());
        isoSpec.setInstallPath(isoTO.getPath());
        isoSpec.setPrimaryStorageUuid(isoTO.getPrimaryStorageUuid());
        isoSpec.setProtocol(isoTO.getProtocol());
        cmd.setIso(convertIsoIfNeeded(isoSpec, host, isoTO));
    }
}
