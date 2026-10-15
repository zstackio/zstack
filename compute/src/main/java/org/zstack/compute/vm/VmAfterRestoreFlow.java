package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
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
public class VmAfterRestoreFlow extends NoRollbackFlow {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        RestoreVmMsg msg = (RestoreVmMsg) data.get(RestoreVmMsg.class.getName());
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<Flow> started = (List<Flow>) data.get(VmRestoreFlow.class.getName());
        List<Flow> flows = new ArrayList<>();
        for (RestoreVmExtensionPoint ext : pluginRgty.getExtensionList(RestoreVmExtensionPoint.class)) {
            Flow flow = ext.getAfterRestoreVmFlow(msg, spec);
            if (flow != null) {
                flows.add(flow);
            }
        }
        VmPreRestoreFlow.runExtensions(new FlowTrigger() {
            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }

            @Override
            public void next() {
                VmInstanceBase vm = (VmInstanceBase) data.get(VmInstanceBase.class.getName());
                VmInstanceInventory original = (VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString());
                boolean originallyRunning = VmInstanceState.Running.toString().equals(original.getState());
                boolean start = msg.getStartVm() == null ? originallyRunning : msg.getStartVm();
                if (!originallyRunning) {
                    if (!start) {
                        trigger.next();
                        return;
                    }
                    data.put(VmRestoreFlow.START_PREPARED, true);
                    vm.refreshVO();
                    VmInstanceInventory inventory = vm.getSelfInventory();
                    ErrorCode error = vm.extEmitter.preStartVm(inventory);
                    if (error != null) {
                        trigger.fail(error);
                        return;
                    }
                    vm.buildSpecFromInventory(inventory, VmInstanceConstant.VmOperation.Start, spec);
                    data.put(VmRestoreFlow.CANDIDATE_HOST, spec.getDestHost());
                    vm.startVm(spec, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                    return;
                }

                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                if (start) {
                    data.put(VmRestoreFlow.START_PREPARED, true);
                    VmInstanceInventory target = spec.getVmInventory();
                    vm.refreshVO();
                    spec.getCdRomSpecs().clear();
                    spec.getHostnames().clear();
                    vm.buildSpecFromInventory(vm.getSelfInventory(), VmInstanceConstant.VmOperation.Reboot, spec);
                    spec.setMessage(null);
                    spec.getVmInventory().setCpuNum(target.getCpuNum());
                    spec.getVmInventory().setMemorySize(target.getMemorySize());
                    chain.then(new VmImageSelectBackupStorageFlow())
                            .then(new VmAllocateNicForStartingVmFlow())
                            .then(new VmInstantiateResourcePreFlow())
                            .then(new VmStartOnHypervisorFlow())
                            .then(new VmInstantiateResourcePostFlow());
                }
                chain.then(new VmSettleRestoreResourceFlow());
                chain.setName(String.format("start-vm-%s-after-restore", vm.self.getUuid()));
                chain.setData(data);
                vm.setFlowMarshaller(chain);
                chain.done(new FlowDoneHandler(trigger) {
                    @Override
                    public void handle(Map ignored) {
                        trigger.next();
                    }
                }).error(new FlowErrorHandler(trigger) {
                    @Override
                    public void handle(ErrorCode errorCode, Map childData) {
                        if (childData.containsKey(VmSettleRestoreResourceFlow.SETTLING)) {
                            trigger.setError(errorCode);
                            // This must remain the last parent flow to report the error without rolling back.
                            trigger.next();
                        } else {
                            trigger.fail(errorCode);
                        }
                    }
                }).start();
            }

            @Override
            public void setError(ErrorCode error) {
                trigger.setError(error);
            }
        }, data, flows, started);
    }

}
