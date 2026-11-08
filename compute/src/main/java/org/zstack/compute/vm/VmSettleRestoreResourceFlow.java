package org.zstack.compute.vm;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.vm.RestoreVmMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceStateEvent;

import java.util.Map;

public class VmSettleRestoreResourceFlow implements Flow {
    static final String SETTLING = VmSettleRestoreResourceFlow.class.getName();

    @Override
    public boolean skip(Map data) {
        return !VmInstanceState.Running.toString().equals(((VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString())).getState());
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceBase vm = (VmInstanceBase) data.get(VmInstanceBase.class.getName());
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        RestoreVmMsg msg = (RestoreVmMsg) data.get(RestoreVmMsg.class.getName());
        if (Boolean.FALSE.equals(msg.getStartVm())) {
            vm.changeVmStateInDb(VmInstanceStateEvent.stopped);
        } else {
            vm.changeVmStateInDb(VmInstanceStateEvent.running);
            vm.syncVmDevicesAddressInfo(vm.self.getUuid());
            new StaticIpOperator().deleteIpChange(vm.self.getUuid());
        }
        data.put(SETTLING, true);
        vm.changeVmResource(spec.getVmInventory());
        vm.fireVmConfigChangedEvent(vm.getSelfInventory());
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (data.containsKey(SETTLING)) {
            trigger.skipRestRollbacks();
            trigger.rollback();
            return;
        }
        VmInstanceBase vm = (VmInstanceBase) data.get(VmInstanceBase.class.getName());
        if (vm.self.getState() == VmInstanceState.Running) {
            vm.changeVmStateInDb(VmInstanceStateEvent.rebooting);
        }
        trigger.rollback();
    }
}
