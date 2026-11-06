package org.zstack.compute.vm;

import org.zstack.compute.vm.devices.TpmLiveMigrationPolicy;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

public class VmMigrateCheckTpmFlow extends NoRollbackFlow {
    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        ErrorCode error = TpmLiveMigrationPolicy.checkHostChange(spec.getVmInventory().getUuid(),
                spec.getVmInventory().getHostUuid(), spec.getDestHost().getUuid());
        if (error != null) {
            trigger.fail(error);
            return;
        }

        trigger.next();
    }
}
