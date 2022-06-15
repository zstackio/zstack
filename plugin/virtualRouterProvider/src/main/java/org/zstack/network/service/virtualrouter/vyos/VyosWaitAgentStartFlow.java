package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmGlobalConfig;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmVO;
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
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.InitCommand;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.InitRsp;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by shixin on 2022/06/15.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosWaitAgentStartFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VyosWaitAgentStartFlow.class);

    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;

    /* this flow is called before VyosDeployAgentFlow, because:
    *  1. vyos vm startup, zvrboot will reboot vyos agent
    *  2. VyosDeployAgentFlow may also reboot vyos agent,
    *  3. we must keep #1 finished before #2
    *  */

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

        String url = vrMgr.buildUrl(mgmtNic.getIp(), VirtualRouterConstant.VR_ECHO_PATH);
        restf.echo(url, new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.next();
            }
        }, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(Long.parseLong(VirtualRouterGlobalConfig.VYOS_ECHO_TIMEOUT.value())));
    }
}
