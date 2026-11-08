package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.RestoreVmExtensionPoint;
import org.zstack.header.vm.RestoreVmMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmBeforeRestoreFlow implements Flow {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<Flow> started = (List<Flow>) data.get(VmPreRestoreFlow.class.getName());
        VmInstanceInventory original = (VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString());
        if (!VmInstanceState.Running.toString().equals(original.getState())) {
            data.put(VmBeforeRestoreFlow.class.getName(), started);
            runExtensions(trigger, data, started);
            return;
        }
        VmInstanceBase vm = (VmInstanceBase) data.get(VmInstanceBase.class.getName());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain()
                .then(new BeforeVmStopOnHypervisorFlow())
                .then(new VmStopOnHypervisorFlow())
                .then(new VmReleaseResourceFlow());
        chain.setName(String.format("stop-vm-%s-for-restore", vm.self.getUuid()));
        chain.setData(data);
        vm.setFlowMarshaller(chain);
        chain.done(new FlowDoneHandler(trigger) {
            @Override
            public void handle(Map ignored) {
                data.put(VmBeforeRestoreFlow.class.getName(), started);
                runExtensions(trigger, data, started);
            }
        }).error(new FlowErrorHandler(trigger) {
            @Override
            public void handle(ErrorCode errorCode, Map ignored) {
                trigger.fail(errorCode);
            }
        }).start();
    }

    private void runExtensions(FlowTrigger trigger, Map data, List<Flow> started) {
        RestoreVmMsg msg = (RestoreVmMsg) data.get(RestoreVmMsg.class.getName());
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<Flow> flows = new ArrayList<>();
        for (RestoreVmExtensionPoint ext : pluginRgty.getExtensionList(RestoreVmExtensionPoint.class)) {
            Flow flow = ext.getBeforeRestoreVmFlow(msg, spec);
            if (flow != null) {
                flows.add(flow);
            }
        }
        VmPreRestoreFlow.runExtensions(trigger, data, flows, started);
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (data.containsKey(RestoreVmExtensionPoint.ROLLBACK_ERROR)) {
            trigger.skipRestRollbacks();
            trigger.rollback();
            return;
        }
        if (!data.containsKey(VmBeforeRestoreFlow.class.getName())) {
            trigger.rollback();
            return;
        }
        VmPreRestoreFlow.rollbackExtensions(trigger, data, VmBeforeRestoreFlow.class.getName());
    }
}
