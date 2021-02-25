package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.*;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.InitCommand;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.InitRsp;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/10/31.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosConnectFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VyosConnectFlow.class);

    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private AnsibleFacade asf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        String vrUuid;
        VmNicInventory mgmtNic;
        if (vr != null) {
            mgmtNic = vr.getManagementNic();
            vrUuid = vr.getUuid();
        } else {
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            vrUuid = spec.getVmInventory().getUuid();
            ApplianceVmInventory applianceVm = ApplianceVmInventory.valueOf(dbf.findByUuid(vrUuid, ApplianceVmVO.class));
            mgmtNic = applianceVm.getManagementNic();
            DebugUtils.Assert(mgmtNic!=null, String.format("cannot find management nic for virtual router[uuid:%s, name:%s]", spec.getVmInventory().getUuid(), spec.getVmInventory().getName()));
        }

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("virtual-router-%s-continue-connecting", mgmtNic.getVmInstanceUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "echo";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String url = vrMgr.buildUrl(mgmtNic.getIp(), VirtualRouterConstant.VR_ECHO_PATH);
                        restf.echo(url, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        }, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(Long.parseLong(VirtualRouterGlobalConfig.VYOS_ECHO_TIMEOUT.value())));
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "init";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String url = vrMgr.buildUrl(mgmtNic.getIp(), VirtualRouterConstant.VR_INIT);

                        int timeoutInSeconds = ApplianceVmGlobalConfig.CONNECT_TIMEOUT.value(Integer.class);
                        int interval = 30 ; /* seonds*/

                        List<Integer> steps = new ArrayList<>(timeoutInSeconds/interval);
                        for (int i = 0; i < timeoutInSeconds/interval; i++) {
                            steps.add(i);
                        }
                        List<ErrorCode> errs = new ArrayList<>();
                        new While<>(steps).each((s, wcompl) -> {
                            InitCommand cmd = new InitCommand();
                            cmd.setMgtCidr(Platform.getManagementServerCidr());
                            cmd.setTimeServers(CoreGlobalProperty.CHRONY_SERVERS);
                            cmd.setUuid(vrUuid);
                            cmd.setLogLevel(rcf.getResourceConfigValue(VirtualRouterGlobalConfig.LOG_LEVEL, vrUuid, String.class));
                            Map <String,String> parms = new HashMap<>();

                            long count = Q.New(VirtualRouterLoadBalancerRefVO.class)
                                .eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, vrUuid)
                                .notNull(VirtualRouterLoadBalancerRefVO_.loadBalancerUuid)
                                .count();
                            if(count > 0){
                                parms.put(VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.getName(),VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.value());
                            }
                            cmd.setParms(parms);


                            restf.asyncJsonPost(url, cmd, new JsonAsyncRESTCallback<InitRsp>(trigger) {
                                private void debug () {
                                    int sshPort = VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class);
                                    if (!NetworkUtils.isRemotePortOpen(mgmtNic.getIp(), sshPort, interval)) {
                                        logger.debug(String.format("vyos agent port %s is not opened on managment nic %s",
                                                sshPort, mgmtNic.getIp()));
                                        return;
                                    }
                                    Ssh ssh1 = new Ssh();
                                    ssh1.setUsername("vyos").setPrivateKey(asf.getPrivateKey()).setPort(sshPort)
                                            .setHostname(mgmtNic.getIp()).setTimeout(interval);
                                    SshResult ret1 = ssh1.command("sudo cat /home/vyos/zvr/zvrboot.log").runAndClose();
                                    if (ret1.getReturnCode() == 0) {
                                        logger.debug(String.format("vyos bootup log %s", ret1.getStdout()));
                                    } else {
                                        logger.debug(String.format("get vyos bootup log failed: %s", ret1.getStderr()));
                                    }

                                    Ssh ssh2 = new Ssh();
                                    ssh2.setUsername("vyos").setPrivateKey(asf.getPrivateKey()).setPort(sshPort)
                                            .setHostname(mgmtNic.getIp()).setTimeout(interval);
                                    SshResult ret2 = ssh2.command("sudo cat /home/vyos/zvr/zvrstartup.log").runAndClose();
                                    if (ret2.getReturnCode() == 0) {
                                        logger.debug(String.format("zvr startup log %s", ret2.getStdout()));
                                    } else {
                                        logger.debug(String.format("get zvr startup log failed: %s", ret2.getStderr()));
                                    }

                                    Ssh ssh3 = new Ssh();
                                    ssh3.setUsername("vyos").setPrivateKey(asf.getPrivateKey()).setPort(sshPort)
                                            .setHostname(mgmtNic.getIp()).setTimeout(interval);
                                    SshResult ret3 = ssh3.command("sudo cat /home/vyos/zvr/zvr.log").runAndClose();
                                    if (ret3.getReturnCode() == 0) {
                                        logger.debug(String.format("zvr log %s", ret3.getStdout()));
                                    } else {
                                        logger.debug(String.format("get zvr log failed: %s", ret3.getStderr()));
                                    }
                                }

                                @Override
                                public void fail(ErrorCode err) {
                                    debug();
                                    errs.add(err);
                                    wcompl.done();
                                }

                                @Override
                                public void success(InitRsp ret) {
                                    if (ret.isSuccess()) {
                                        VirtualRouterMetadataStruct struct = new VirtualRouterMetadataStruct();
                                        struct.setVrUuid(vrUuid);
                                        struct.setKernelVersion(ret.getKernelVersion());
                                        struct.setVyosVersion(ret.getVyosVersion());
                                        struct.setZvrVersion(ret.getZvrVersion());
                                        new VirtualRouterMetadataOperator().updateVirtualRouterMetadata(struct);
                                        errs.removeAll(errs);
                                        wcompl.allDone();
                                    } else {
                                        debug();
                                        errs.add(operr("vyos init command failed, because:%s", ret.getError()));
                                        wcompl.done();
                                    }
                                }

                                @Override
                                public Class<InitRsp> getReturnClass() {
                                    return InitRsp.class;
                                }
                            }, TimeUnit.SECONDS, interval);
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                if (errs.isEmpty()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errs.get(errs.size() - 1));
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map data) {
                        trigger.next();
                    }
                });

                error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        trigger.fail(errCode);
                    }
                });
            }
        }).start();
    }
}
