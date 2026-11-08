package org.zstack.compute.vm;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.CheckVmStateOnHypervisorMsg;
import org.zstack.header.host.CheckVmStateOnHypervisorReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.RestoreVmExtensionPoint;
import org.zstack.header.vm.RestoreVmMsg;
import org.zstack.header.vm.StopVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceStateEvent;

import java.util.Map;

import static org.zstack.header.vm.RestoreVmExtensionPoint.UNSAFE_TO_START_VM;
import static org.zstack.header.vm.RestoreVmExtensionPoint.ROLLBACK_ERROR;
import static org.zstack.utils.CollectionDSL.list;

public class VmReserveResourceForRestoreFlow implements Flow {
    private static final String RESERVED = VmReserveResourceForRestoreFlow.class.getName();

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        VmInstanceInventory original = (VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString());
        if (!VmInstanceState.Running.toString().equals(original.getState())) {
            trigger.next();
            return;
        }
        VmInstanceBase vm = (VmInstanceBase) data.get(VmInstanceBase.class.getName());
        vm.changeVmResource(spec.getVmInventory());
        data.put(RESERVED, true);
        vm.changeVmStateInDb(VmInstanceStateEvent.rebooting);
        vm.buildSpecFromInventory(vm.getSelfInventory(), VmInstanceConstant.VmOperation.Reboot, spec);
        StopVmInstanceMsg stop = new StopVmInstanceMsg();
        stop.setVmInstanceUuid(vm.self.getUuid());
        spec.setMessage(stop);
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (!data.containsKey(RESERVED)) {
            trigger.rollback();
            return;
        }
        VmInstanceBase vm = (VmInstanceBase) data.get(VmInstanceBase.class.getName());
        VmInstanceInventory original = (VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString());
        Completion completed = new Completion(null) {
            @Override
            public void success() {
                trigger.rollback();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                data.put(ROLLBACK_ERROR, errorCode);
                trigger.skipRestRollbacks();
                trigger.rollback();
            }
        };
        NoErrorCompletion reconcile = new NoErrorCompletion(completed) {
            @Override
            public void done() {
                CheckVmStateOnHypervisorMsg cmsg = new CheckVmStateOnHypervisorMsg();
                cmsg.setVmInstanceUuids(list(vm.self.getUuid()));
                cmsg.setHostUuid(original.getHostUuid());
                vm.bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, original.getHostUuid());
                vm.bus.send(cmsg, new CloudBusCallBack(completed) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            data.put(ROLLBACK_ERROR, reply.getError());
                            trigger.skipRestRollbacks();
                        }
                        String state = reply.isSuccess() ? ((CheckVmStateOnHypervisorReply) reply).getStates().get(vm.self.getUuid()) : null;
                        vm.refreshVO();
                        if (!VmInstanceState.Running.toString().equals(state)
                                && (data.containsKey(ROLLBACK_ERROR) || Boolean.TRUE.equals(data.get(UNSAFE_TO_START_VM)))) {
                            for (RestoreVmExtensionPoint ext : vm.pluginRgty.getExtensionList(RestoreVmExtensionPoint.class)) {
                                ext.failedToRecoverVmAfterRestore((RestoreVmMsg) data.get(RestoreVmMsg.class.getName()));
                            }
                        }
                        if (!VmInstanceState.Running.toString().equals(state) && !VmInstanceState.Stopped.toString().equals(state)) {
                            VmInstanceBase.logger.warn(String.format("cannot confirm the state of vm[uuid:%s] on host[uuid:%s] after restore failure, keep its host and reserved capacity, %s",
                                    vm.self.getUuid(), original.getHostUuid(), reply.isSuccess() ? state : reply.getError()));
                            vm.changeVmStateInDb(VmInstanceStateEvent.unknown, () -> vm.self.setHostUuid(original.getHostUuid()));
                        } else {
                            vm.changeVmStateInDb(VmInstanceState.Running.toString().equals(state)
                                    ? VmInstanceStateEvent.running : VmInstanceStateEvent.stopped, () -> vm.self.setHostUuid(original.getHostUuid()));
                            vm.changeVmResource(original);
                        }
                        completed.success();
                    }
                });
            }
        };

        if (!data.containsKey(VmBeforeRestoreFlow.class.getName()) || Boolean.TRUE.equals(data.get(UNSAFE_TO_START_VM))) {
            reconcile.done();
            return;
        }

        new NoErrorCompletion(reconcile) {
            @Override
            public void done() {
                vm.refreshVO();
                VmInstanceSpec startSpec = vm.buildSpecFromInventory(vm.getSelfInventory(), VmInstanceConstant.VmOperation.Reboot);
                startSpec.getVmInventory().setCpuNum(original.getCpuNum());
                startSpec.getVmInventory().setMemorySize(original.getMemorySize());
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain()
                        .then(new VmImageSelectBackupStorageFlow())
                        .then(new VmAllocateNicForStartingVmFlow())
                        .then(new VmInstantiateResourcePreFlow())
                        .then(new VmStartOnHypervisorFlow())
                        .then(new VmInstantiateResourcePostFlow());
                chain.setName(String.format("restart-vm-%s-after-restore-failure", vm.self.getUuid()));
                vm.setFlowMarshaller(chain);
                chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), startSpec);
                chain.done(new FlowDoneHandler(reconcile) {
                    @Override
                    public void handle(Map ignored) {
                        vm.syncVmDevicesAddressInfo(vm.self.getUuid());
                        new StaticIpOperator().deleteIpChange(vm.self.getUuid());
                        vm.refreshVO();
                        vm.changeVmStateInDb(VmInstanceStateEvent.running, () -> vm.self.setHostUuid(original.getHostUuid()));
                        new NoErrorCompletion(completed) {
                            @Override
                            public void done() {
                                vm.changeVmResource(original);
                                completed.success();
                            }
                        }.done();
                    }
                }).error(new FlowErrorHandler(reconcile) {
                    @Override
                    public void handle(ErrorCode errorCode, Map ignored) {
                        VmInstanceBase.logger.warn(String.format("failed to start vm[uuid:%s] with its original configuration after restore failure, %s",
                                vm.self.getUuid(), errorCode));
                        data.put(ROLLBACK_ERROR, errorCode);
                        trigger.skipRestRollbacks();
                        completed.success();
                    }
                }).start();
            }
        }.done();
    }
}
