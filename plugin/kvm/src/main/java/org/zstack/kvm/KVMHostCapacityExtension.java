package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.UnableToReserveHostCapacityException;
import org.zstack.header.cluster.ReportHostCapacityMessage;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMAgentCommands.HostCapacityCmd;
import org.zstack.kvm.KVMAgentCommands.HostCapacityResponse;
import org.zstack.utils.SizeUtils;

import java.util.Map;

import static org.zstack.core.Platform.operr;

public class KVMHostCapacityExtension implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private void reportCapacity(HostInventory host, Completion completion) {
        KVMHostSyncHttpCallMsg msg = new KVMHostSyncHttpCallMsg();
        msg.setHostUuid(host.getUuid());
        msg.setPath(KVMConstant.KVM_HOST_CAPACITY_PATH);
        msg.setNoStatusCheck(true);
        msg.setCommand(new HostCapacityCmd());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        KVMHostSyncHttpCallReply r = reply.castReply();
        HostCapacityResponse rsp = r.toResponse(HostCapacityResponse.class);
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(operr(rsp.getError()));
        }

        if (rsp.getTotalMemory() < SizeUtils.sizeStringToBytes(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.value())) {
            throw new UnableToReserveHostCapacityException(String.format("The host[uuid:%s]'s memory capacity[%s] is lower than the minimal required capacity[%s]",
                    host.getUuid(), rsp.getTotalMemory(), SizeUtils.sizeStringToBytes(KVMGlobalConfig.RESERVED_MEMORY_CAPACITY.value())));
        }

        ReportHostCapacityMessage rmsg = new ReportHostCapacityMessage();
        rmsg.setHostUuid(host.getUuid());
        rmsg.setCpuNum((int) rsp.getCpuNum());
        rmsg.setUsedCpu(rsp.getUsedCpu());
        rmsg.setTotalMemory(rsp.getTotalMemory());
        rmsg.setUsedMemory(rsp.getUsedMemory());
        rmsg.setCpuSockets(rsp.getCpuSockets());
        rmsg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        bus.send(rmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }


    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        reportCapacity(inv, new NopeCompletion());
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "sync-host-capacity";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                reportCapacity(context.getInventory(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }
}
