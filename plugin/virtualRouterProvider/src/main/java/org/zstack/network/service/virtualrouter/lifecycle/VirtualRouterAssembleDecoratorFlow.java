package org.zstack.network.service.virtualrouter.lifecycle;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmPostLifeCycleInfo;
import org.zstack.appliancevm.ApplianceVmSpec;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterAssembleDecoratorFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        ApplianceVmPostLifeCycleInfo info;
        if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
            final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);

            info = new ApplianceVmPostLifeCycleInfo();
            info.setDefaultRouteL3Network(aspec.getDefaultRouteL3Network());
            VmNicInventory mgmtNic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                @Override
                public VmNicInventory call(VmNicInventory arg) {
                    return arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()) ? arg : null;
                }
            });
            info.setManagementNic(mgmtNic);
            data.put(Param.IS_NEW_CREATED.toString(), true);
        } else {
            info = (ApplianceVmPostLifeCycleInfo) data.get(ApplianceVmConstant.Params.applianceVmInfoForPostLifeCycle.toString());
        }

        if (spec.getCurrentVmOperation() == VmOperation.Destroy) {
            // for destroy, the VR information may not be complete(e.g mgmt node crash when VR is starting)
            // only need uuid for cleanup
            data.put(Param.VR_UUID.toString(), spec.getVmInventory().getUuid());
        } else {
            data.put(VirtualRouterConstant.Param.VR.toString(), VirtualRouterVmInventory.valueOf(dbf.findByUuid(spec.getVmInventory().getUuid(), VirtualRouterVmVO.class)));
        }
        trigger.next();
    }
}
