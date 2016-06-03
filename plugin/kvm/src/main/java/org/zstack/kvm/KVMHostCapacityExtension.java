package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.logging.Log;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.cluster.ReportHostCapacityMessage;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMAgentCommands.HostCapacityCmd;
import org.zstack.kvm.KVMAgentCommands.HostCapacityResponse;

import java.util.Map;

public class KVMHostCapacityExtension implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private void reportCapacity(HostInventory host) {
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
            throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
        }
        ReportHostCapacityMessage rmsg = new ReportHostCapacityMessage();
        rmsg.setHostUuid(host.getUuid());
        rmsg.setCpuNum((int) rsp.getCpuNum());
        rmsg.setUsedCpu(rsp.getUsedCpu());
        rmsg.setTotalMemory(rsp.getTotalMemory());
        rmsg.setUsedMemory(rsp.getUsedMemory());
        rmsg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        bus.send(rmsg);
    }


    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        reportCapacity(inv);
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
                new Log(context.getInventory().getUuid()).log(KVMHostLabel.SYNC_HOST_CAPACITY);

                reportCapacity(context.getInventory());
                trigger.next();
            }
        };
    }
}
