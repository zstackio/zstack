package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.cluster.ReportHostCapacityMessage;
import org.zstack.header.host.HostConnectionReestablishExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.KVMAgentCommands.HostCapacityCmd;
import org.zstack.kvm.KVMAgentCommands.HostCapacityResponse;

public class KVMHostCapacityExtension implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private KVMHostFactory factory;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void kvmHostConnected(KVMHostConnectedContext context) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(context.getBaseUrl());
        ub.path(KVMConstant.KVM_HOST_CAPACITY_PATH);
        String url = ub.build().toUriString();
        HostCapacityCmd cmd = new HostCapacityCmd();
        HostCapacityResponse rsp = restf.syncJsonPost(url, cmd, HostCapacityResponse.class);
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
        }

        ReportHostCapacityMessage msg = new ReportHostCapacityMessage();
        msg.setHostUuid(context.getInventory().getUuid());
        msg.setTotalCpu(rsp.getCpuNum() * rsp.getCpuSpeed());
        msg.setUsedCpu(rsp.getUsedCpu());
        msg.setTotalMemory(rsp.getTotalMemory());
        msg.setUsedMemory(rsp.getUsedMemory());
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        bus.send(msg);
    }

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        KVMHostConnectedContext ctx = new KVMHostConnectedContext(factory.getHostContext(inv.getUuid()), false);
        kvmHostConnected(ctx);
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }
}
