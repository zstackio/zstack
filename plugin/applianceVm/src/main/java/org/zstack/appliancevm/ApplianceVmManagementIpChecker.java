package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmBeforeCreateOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmBeforeStartOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.err;

/**
 */
public class ApplianceVmManagementIpChecker implements VmBeforeCreateOnHypervisorExtensionPoint, VmBeforeStartOnHypervisorExtensionPoint {

    @Autowired
    private DatabaseFacade dbf;

    private void checkManagementIp(VmInstanceSpec spec, boolean isNewCreated) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        if (!ApplianceVmConstant.APPLIANCE_VM_TYPE.equals(spec.getVmInventory().getType())) {
            return;
        }

        VmNicInventory mgmtNic;
        if (isNewCreated) {
            final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
            mgmtNic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                @Override
                public VmNicInventory call(VmNicInventory arg) {
                    return arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()) ? arg : null;
                }
            });
        } else {
            ApplianceVmInventory apvm = ApplianceVmInventory.valueOf(dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class));
            mgmtNic = apvm.getManagementNic();
        }

        DebugUtils.Assert(mgmtNic!=null, String.format("cannot find management nic of appliance vm[uuid:%s, newCreated: %s]", spec.getVmInventory().getUuid(), isNewCreated));

        if (NetworkUtils.isReachable(mgmtNic.getIp(), 1000)) {
            throw new OperationFailureException(err(ApplianceVmErrors.MANAGEMENT_IP_OCCUPIED,
                    "the management nic IP[%s] has been occupied by another device in the data center, we can ping it", mgmtNic.getIp()
            ));
        }
    }

    @Override
    public void beforeCreateVmOnHypervisor(VmInstanceSpec spec) {
        checkManagementIp(spec, true);
    }

    @Override
    public void beforeStartVmOnHypervisor(VmInstanceSpec spec) {
        checkManagementIp(spec, false);
    }
}
