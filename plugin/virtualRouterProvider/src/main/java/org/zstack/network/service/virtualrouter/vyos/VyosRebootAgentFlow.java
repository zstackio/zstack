package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosRebootAgentFlow extends VyosRunScriptFlow {
    private static final CLogger logger = Utils.getLogger(VyosRebootAgentFlow.class);
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    protected RESTFacade restf;

    @Override
    public void initEnv() {
        setLogger(Utils.getLogger(VyosRebootAgentFlow.class));
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        init(data);

        if (isSkipRunningScript(data)) {
            trigger.next();
            return;
        }

        initEnv();

        createScript();

        beforeExecuteScript();

        //if not new create, reboot vyos
        boolean isNewCreate = Boolean.parseBoolean((String) data.getOrDefault(ApplianceVmConstant.Params.isNewCreate.toString(), "false"));
        if (!isNewCreate) {
            executeScript(new Completion(trigger) {
                @Override
                public void success() {
                    afterExecuteScript();
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                    afterExecuteScriptFail();
                }
            });
            return;
        }

        checkVersion(data, new ReturnValueCompletion<VyosVersionCheckResult>(trigger) {
            @Override
            public void success(VyosVersionCheckResult returnValue) {
                if (!returnValue.needReconnect) {
                    logger.debug("not need to reboot agent after check version");
                    trigger.next();
                    return;
                }
                executeScript(new Completion(trigger) {
                    @Override
                    public void success() {
                        afterExecuteScript();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                        afterExecuteScriptFail();
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });

    }

    @Override
    public boolean isSkipRunningScript(Map data) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }

        boolean needRebootAgent = Boolean.parseBoolean((String) data.getOrDefault(ApplianceVmConstant.Params.needRebootAgent.toString(), "false"));
        boolean isNewCreate = Boolean.parseBoolean((String) data.getOrDefault(ApplianceVmConstant.Params.isNewCreate.toString(), "false"));
        // no need to reboot agent
        return !needRebootAgent && !isNewCreate;
    }

    @Override
    public void createScript() {
        String script = "sudo bash /home/vyos/zvrboot.bin\n" +
                "sudo bash /home/vyos/zvr.bin\n" +
                "sudo bash /etc/init.d/zstack-virtualrouteragent restart\n";
        super.script(script);
    }

    @Override
    public String getTaskName() {
        return VyosRebootAgentFlow.class.getName();
    }

    @Override
    public String getScriptName() {
        return "vyos reboot";
    }

    @Override
    public void afterExecuteScript() {
        if (getVrUuid() != null) {
            updateZvrVersion(getVrUuid());
        }
    }


    private void updateZvrVersion(String vrUuid) {
        VirtualRouterMetadataVO vo = dbf.findByUuid(vrUuid, VirtualRouterMetadataVO.class);
        String managementVersion = getManagementVersion();
        if (managementVersion == null) {
            return;
        }

        if (vo != null) {
            vo.setZvrVersion(managementVersion);
            dbf.update(vo);
        } else {
            vo = new VirtualRouterMetadataVO();
            vo.setUuid(vrUuid);
            dbf.persist(vo);
        }
    }

    private String getManagementVersion() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "3.10.0.0";
        }

        String managementVersion;
        String path;
        try {
            path = PathUtil.findFileOnClassPath(VyosConstants.VYOS_VERSION_PATH, true).getAbsolutePath();
        } catch (RuntimeException e) {
            logger.error(String.format("vyos version file find file because %s", e.getMessage()));
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            managementVersion = br.readLine();
        } catch (IOException e) {
            logger.error(String.format("vyos version file %s read error: %s", path, e.getMessage()));
            return null;
        }

        if (!(VirtualRouterMetadataOperator.zvrVersionCheck(managementVersion))) {
            logger.error(String.format("vyos version file format error: %s", managementVersion));
            return null;
        }

        return managementVersion;
    }

    private void checkVersion(Map flowData, ReturnValueCompletion<VyosVersionCheckResult> completion) {
        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("virtual-router-%s-get-version", vrUuid));
        chain.setData(flowData);
        chain.then(new ShareFlow() {
            Boolean echoSuccess = false;
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "echo";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String url = vrMgr.buildUrl(mgmtNicIp, VirtualRouterConstant.VR_ECHO_PATH);
                        restf.echo(url, new Completion(trigger) {
                            @Override
                            public void success() {
                                echoSuccess = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                if (NetworkUtils.isRemotePortOpen(mgmtNicIp, 22, 2000)) {
                                    trigger.next();
                                } else {
                                    trigger.fail(errorCode);
                                }
                            }
                        }, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(Long.parseLong(VirtualRouterGlobalConfig.VYOS_ECHO_TIMEOUT.value())));
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "get-version";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VyosVersionCheckResult result = new VyosVersionCheckResult();
                        if (!echoSuccess) {
                            flowData.put(VyosVersionCheckResult.class.toString(), result);
                            trigger.next();
                            return;
                        }

                        vyosRouterVersionCheck(new ReturnValueCompletion<VyosVersionCheckResult>(trigger) {
                            @Override
                            public void success(VyosVersionCheckResult returnValue) {
                                flowData.put(VyosVersionCheckResult.class.toString(), returnValue);
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
                        completion.success((VyosVersionCheckResult) flowData.get(VyosVersionCheckResult.class.toString()));
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

    public void vyosRouterVersionCheck(ReturnValueCompletion<VyosVersionCheckResult> completion) {
        VyosVersionCheckResult result = new VyosVersionCheckResult();

        String managementVersion = getManagementVersion();
        if (managementVersion == null) {
            completion.success(result);
            return;
        }

        VirtualRouterCommands.PingCmd cmd = new VirtualRouterCommands.PingCmd();
        cmd.setUuid(vrUuid);
        restf.asyncJsonPost(vrMgr.buildUrl(mgmtNicIp, VirtualRouterConstant.VR_PING), cmd, null, new JsonAsyncRESTCallback<VirtualRouterCommands.PingRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                logger.warn(String.format("virtual router[uuid: %s] get version failed because %s", vrUuid, err.getDetails()));
                completion.success(result);
            }

            @Override
            public void success(VirtualRouterCommands.PingRsp ret) {
                if (!ret.isSuccess()){
                    logger.warn(String.format("virtual router[uuid: %s] failed to get version because %s", vrUuid, ret.getError()));
                    result.setNeedReconnect(true);
                    completion.success(result);
                    return;
                }

                if (ret.getVersion() == null) {
                    logger.warn(String.format("virtual router[uuid: %s] doesn't have version", vrUuid));
                    result.setNeedReconnect(true);
                    completion.success(result);
                    return;
                }

                if (!(VirtualRouterMetadataOperator.zvrVersionCheck(ret.getVersion()))) {
                    logger.warn(String.format("virtual router[uuid: %s] version [%s] format error", vrUuid, ret.getVersion()));
                    result.setNeedReconnect(true);
                    completion.success(result);
                    return;
                }

                VersionComparator mnVersion = new VersionComparator(managementVersion);
                VersionComparator remoteVersion = new VersionComparator(ret.getVersion());
                result.setVersion(ret.getVersion());
                if (mnVersion.compare(remoteVersion) > 0) {
                    logger.warn(String.format("virtual router[uuid: %s] version [%s] is older than management node version [%s]",vrUuid, ret.getVersion(), managementVersion));
                    result.setNeedReconnect(true);
                    completion.success(result);
                } else {
                    logger.debug(String.format("virtual router[uuid: %s] successfully finish the version check", vrUuid));
                    completion.success(result);
                }
            }

            @Override
            public Class<VirtualRouterCommands.PingRsp> getReturnClass() {
                return VirtualRouterCommands.PingRsp.class;
            }
        });
    }
}
