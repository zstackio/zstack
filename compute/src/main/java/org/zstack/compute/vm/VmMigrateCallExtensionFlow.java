package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmMigrateCallExtensionFlow implements Flow {
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        HostInventory destHost = spec.getDestHost();
        ErrorCode err = extEmitter.preMigrateVm(spec.getVmInventory(), destHost.getUuid());
        if (err != null) {
            trigger.fail(err);
            return;
        }

        extEmitter.beforeMigrateVm(spec.getVmInventory(), destHost.getUuid());
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        trigger.rollback();
    }
}
