package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
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
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.RestoreVmExtensionPoint;
import org.zstack.header.vm.RestoreVmMsg;
import org.zstack.header.vm.RestoreVmReply;
import org.zstack.header.vm.StopVmInstanceMsg;
import org.zstack.header.vm.StopVmType;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceStateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.header.vm.RestoreVmExtensionPoint.ROLLBACK_ERROR;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmRestoreFlow implements Flow {
    static final String START_PREPARED = VmRestoreFlow.class.getName() + ".startPrepared";
    static final String CANDIDATE_HOST = VmRestoreFlow.class.getName() + ".candidateHost";

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<Flow> started = new ArrayList<>();
        data.put(VmRestoreFlow.class.getName(), started);
        RestoreVmMsg msg = (RestoreVmMsg) data.get(RestoreVmMsg.class.getName());
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        RestoreVmReply reply = (RestoreVmReply) data.get(RestoreVmReply.class.getName());
        List<Flow> flows = new ArrayList<>();
        for (RestoreVmExtensionPoint ext : pluginRgty.getExtensionList(RestoreVmExtensionPoint.class)) {
            Flow flow = ext.getRestoreVmFlow(msg, spec, reply);
            if (flow != null) {
                flows.add(flow);
            }
        }
        VmPreRestoreFlow.runExtensions(trigger, data, flows, started);
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        VmInstanceBase vm = (VmInstanceBase) data.get(VmInstanceBase.class.getName());
        VmInstanceInventory original = (VmInstanceInventory) data.get(VmInstanceConstant.Params.vmInventory.toString());
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        boolean originallyRunning = VmInstanceState.Running.toString().equals(original.getState());
        HostInventory destHost = spec.getDestHost();
        String hostUuid = data.containsKey(START_PREPARED)
                ? (originallyRunning ? original.getHostUuid()
                    : destHost != null && (data.containsKey(ROLLBACK_ERROR)
                        || data.containsKey(CANDIDATE_HOST) && destHost != data.get(CANDIDATE_HOST)) ? destHost.getUuid() : null)
                : null;
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
        NoErrorCompletion unknown = new NoErrorCompletion(completed) {
            @Override
            public void done() {
                trigger.skipRestRollbacks();
                VmInstanceBase.logger.warn(String.format("cannot confirm vm[uuid:%s] is stopped before restore compensation, keep its host and capacity",
                        vm.self.getUuid()));
                vm.refreshVO();
                for (RestoreVmExtensionPoint ext : pluginRgty.getExtensionList(RestoreVmExtensionPoint.class)) {
                    ext.failedToRecoverVmAfterRestore((RestoreVmMsg) data.get(RestoreVmMsg.class.getName()));
                }
                vm.changeVmStateInDb(VmInstanceStateEvent.unknown, () -> vm.self.setHostUuid(hostUuid));
                completed.success();
            }
        };
        if (data.containsKey(ROLLBACK_ERROR)) {
            unknown.done();
            return;
        }
        NoErrorCompletion compensated = new NoErrorCompletion(unknown) {
            @Override
            public void done() {
                VmPreRestoreFlow.rollbackExtensions(trigger, data, VmRestoreFlow.class.getName());
            }
        };
        NoErrorCompletion stopped = new NoErrorCompletion(unknown) {
            @Override
            public void done() {
                vm.refreshVO();
                if (originallyRunning) {
                    if (vm.self.getState() == VmInstanceState.Running) {
                        vm.changeVmStateInDb(VmInstanceStateEvent.rebooting, () -> vm.self.setHostUuid(hostUuid));
                    }
                } else {
                    vm.changeVmStateInDb(VmInstanceStateEvent.stopped);
                }
                compensated.done();
            }
        };
        if (hostUuid == null) {
            compensated.done();
            return;
        }
        CheckVmStateOnHypervisorMsg cmsg = new CheckVmStateOnHypervisorMsg();
        cmsg.setVmInstanceUuids(list(vm.self.getUuid()));
        cmsg.setHostUuid(hostUuid);
        vm.bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, hostUuid);
        vm.bus.send(cmsg, new CloudBusCallBack(unknown) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    VmInstanceBase.logger.warn(String.format("unable to check state of the vm[uuid:%s] on the host[uuid:%s], treat it as Unknown, %s",
                            vm.self.getUuid(), hostUuid, reply.getError()));
                    data.put(ROLLBACK_ERROR, reply.getError());
                }
                String state = reply.isSuccess() ? ((CheckVmStateOnHypervisorReply) reply).getStates().get(vm.self.getUuid()) : null;
                if (VmInstanceState.Stopped.toString().equals(state)) {
                    stopped.done();
                    return;
                }
                if (!VmInstanceState.Running.toString().equals(state) && !VmInstanceState.Paused.toString().equals(state)) {
                    unknown.done();
                    return;
                }
                VmInstanceInventory inventory = vm.getSelfInventory();
                inventory.setHostUuid(hostUuid);
                VmInstanceSpec stopSpec = vm.buildSpecFromInventory(inventory, VmInstanceConstant.VmOperation.Reboot);
                StopVmInstanceMsg stop = new StopVmInstanceMsg();
                stop.setVmInstanceUuid(vm.self.getUuid());
                stop.setType(StopVmType.cold.toString());
                stopSpec.setMessage(stop);
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain()
                        .then(new BeforeVmStopOnHypervisorFlow())
                        .then(new VmStopOnHypervisorFlow())
                        .then(new VmReleaseResourceFlow());
                chain.setName(String.format("stop-vm-%s-before-restore-compensation", vm.self.getUuid()));
                vm.setFlowMarshaller(chain);
                chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), stopSpec);
                chain.done(new FlowDoneHandler(stopped) {
                    @Override
                    public void handle(Map ignored) {
                        stopped.done();
                    }
                }).error(new FlowErrorHandler(unknown) {
                    @Override
                    public void handle(ErrorCode errorCode, Map ignored) {
                        VmInstanceBase.logger.warn(String.format("failed to stop VM before restore compensation, %s", errorCode));
                        data.put(ROLLBACK_ERROR, errorCode);
                        unknown.done();
                    }
                }).start();
            }
        });
    }
}
