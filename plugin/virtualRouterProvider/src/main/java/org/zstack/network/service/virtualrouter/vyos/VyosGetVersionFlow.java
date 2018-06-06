package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSpec;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by shixin.ruan on 2018/05/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosGetVersionFlow extends NoRollbackFlow {
    private final static CLogger logger = Utils.getLogger(VyosGetVersionFlow.class);
    @Autowired
    private VyosVersionManager vyosVersionManager;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger flowTrigger, Map flowData) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) flowData.get(VirtualRouterConstant.Param.VR.toString());
        String vrUuid;
        VmNicInventory mgmtNic;

        if (vr != null) {
            mgmtNic = vr.getManagementNic();
            vrUuid = vr.getUuid();
        } else {
            final VmInstanceSpec spec = (VmInstanceSpec) flowData.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            vrUuid = spec.getVmInventory().getUuid();
            if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        return arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()) ? arg : null;
                    }
                });
            } else {
                ApplianceVmVO avo = dbf.findByUuid(vrUuid, ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
            }
        }

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("virtual-router-%s-get-version", vrUuid));
        chain.setData(flowData);
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
                    String __name__ = "get-version";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        vyosVersionManager.vyosRouterVersionCheck(vrUuid, new Completion(trigger) {
                            @Override
                            public void success() {
                                logger.debug(String.format("virtual router [uuid:%s] version check successfully", vrUuid));
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("virtual router [uuid:%s] version check failed because %s, need to be reconnect", vrUuid, errorCode.getDetails()));
                                flowData.put(ApplianceVmConstant.Params.isReconnect.toString(), Boolean.TRUE.toString());
                                flowData.put(ApplianceVmConstant.Params.managementNicIp.toString(), mgmtNic.getIp());
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(flowTrigger) {
                    @Override
                    public void handle(Map data) {
                        flowTrigger.next();
                    }
                });

                error(new FlowErrorHandler(flowTrigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        flowTrigger.fail(errCode);
                    }
                });
            }
        }).start();
    }
}
