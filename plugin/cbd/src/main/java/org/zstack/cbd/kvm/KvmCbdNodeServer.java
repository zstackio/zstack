package org.zstack.cbd.kvm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.cbd.AddonInfo;
import org.zstack.cbd.CbdConstants;
import org.zstack.cbd.MdsInfo;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.externalStorage.primary.ExternalStorageConstant;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.HeartbeatVolumeTO;
import org.zstack.header.storage.addon.primary.PrimaryStorageNodeSvc;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.cbd.kvm.KvmCbdCommands.AgentRsp;
import org.zstack.cbd.kvm.KvmCbdCommands.KvmSetupSelfFencerCmd;
import org.zstack.cbd.kvm.KvmCbdCommands.KvmUpdateClientConfCmd;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * @author Xingwei Yu
 * @date 2024/4/9 16:22
 */
public class KvmCbdNodeServer implements Component, KvmSetupSelfFencerExtensionPoint {
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
            HeartbeatVolumeTO heartbeatVol;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    final String __name__ = "configure-cbd-client-on-kvm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ExternalPrimaryStorageVO vo = dbf.findByUuid(param.getPrimaryStorage().getUuid(), ExternalPrimaryStorageVO.class);
                        if (vo == null) {
                            trigger.fail(operr("not found primary storage[uuid:%s].", param.getPrimaryStorage().getUuid()));
                            return;
                        }

                        KvmUpdateClientConfCmd cmd = new KvmUpdateClientConfCmd();
                        AddonInfo addonInfo = StringUtils.isEmpty(vo.getAddonInfo()) ? new AddonInfo() : JSONObjectUtil.toObject(vo.getAddonInfo(), AddonInfo.class);
                        List<MdsInfo> mdsInfos = new ArrayList<>();
                        for (MdsInfo mdsInfo : addonInfo.getMdsInfos()) {
                            MdsInfo info = new MdsInfo();
                            info.setMdsExternalAddr(mdsInfo.getMdsExternalAddr());
                            mdsInfos.add(info);
                        }
                        cmd.setMdsInfos(mdsInfos);

                        httpCall(KvmCbdCommands.CBD_CONFIGURE_CLIENT_PATH, param.getHostUuid(), cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
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

                flow(new NoRollbackFlow() {
                    final String __name__ = "activate-cbd-heartbeat-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        nodeSvc.activateHeartbeatVolume(host, new ReturnValueCompletion<HeartbeatVolumeTO>(trigger) {
                            @Override
                            public void success(HeartbeatVolumeTO returnValue) {
                                heartbeatVol = returnValue;
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
                        KvmSetupSelfFencerCmd cmd = new KvmSetupSelfFencerCmd();
                        cmd.interval = param.getInterval();
                        cmd.maxAttempts = param.getMaxAttempts();
                        cmd.coveringPaths = heartbeatVol.getCoveringPaths();
                        cmd.heartbeatUrl = heartbeatVol.getInstallPath();
                        cmd.storageCheckerTimeout = param.getStorageCheckerTimeout();
                        cmd.heartbeatRequiredSpace = heartbeatVol.getHeartbeatRequiredSpace();
                        cmd.hostUuid = param.getHostUuid();
                        cmd.strategy = param.getStrategy();
                        cmd.uuid = param.getPrimaryStorage().getUuid();
                        cmd.fencers = param.getFencers();

                        httpCall(KvmCbdCommands.CBD_SETUP_SELF_FENCER_PATH, param.getHostUuid(), cmd, true, AgentRsp.class, new ReturnValueCompletion<AgentRsp>(trigger) {
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
}
