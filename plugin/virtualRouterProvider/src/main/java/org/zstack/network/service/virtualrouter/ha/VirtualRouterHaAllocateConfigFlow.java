package org.zstack.network.service.virtualrouter.ha;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterHaAllocateConfigFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        VirtualRouterVmVO vrVO = dbf.findByUuid(spec.getVmInventory().getUuid(), VirtualRouterVmVO.class);
        if (vrVO != null && vrVO.isHaEnabled()) {
            List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
            if (exps.isEmpty()) {
                trigger.next();
                return;
            }

            exps.get(0).allocateVirtualRouterHaConfig(vrVO.getUuid(), new Completion(trigger) {
                @Override
                public void success() {
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.fail(errorCode);
                }
            });
        } else {
            trigger.next();
        }

    }
}
