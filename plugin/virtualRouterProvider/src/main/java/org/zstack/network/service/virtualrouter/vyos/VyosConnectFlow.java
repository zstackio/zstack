package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.*;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.InitCommand;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.InitRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/10/31.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosConnectFlow extends NoRollbackFlow {
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VyosVersionManager vyosVersionManager;
    @Autowired
    private PluginRegistry pluginRgty;
    private static final CLogger logger = Utils.getLogger(VyosConnectFlow.class);

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
                        InitCommand cmd = new InitCommand();

                        cmd.setMgtCidr(Platform.getManagementServerCidr());
                        cmd.setTimeServers(CoreGlobalProperty.CHRONY_SERVERS);
                        cmd.setUuid(vrUuid);
                        cmd.setLogLevel(rcf.getResourceConfigValue(VirtualRouterGlobalConfig.LOG_LEVEL, vrUuid, String.class));
                        restf.asyncJsonPost(url, cmd, new JsonAsyncRESTCallback<InitRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(InitRsp ret) {
                                if (ret.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(operr("operation error, because:%s", ret.getError()));
                                }
                            }

                            @Override
                            public Class<InitRsp> getReturnClass() {
                                return InitRsp.class;
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-version-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vyosVersionManager.vyosRouterVersionCheck(vrUuid, new ReturnValueCompletion<VyosVersionCheckResult>(trigger) {
                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.next();
                            }

                            @Override
                            public void success(VyosVersionCheckResult returnValue) {
                                if (returnValue.isNeedReconnect()) {
                                    trigger.next();
                                    return;
                                }

                                List<VyosConnectExtensionPoint> exts = pluginRgty.getExtensionList(VyosConnectExtensionPoint.class);
                                if (exts.isEmpty()) {
                                    trigger.next();
                                    return;
                                }
                                logger.debug(String.format("start to sync version to db, general zvr version is %s", returnValue.getVersion()));
                                exts.get(0).syncVersionToDb(vrUuid, returnValue.getVersion());
                                trigger.next();
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
