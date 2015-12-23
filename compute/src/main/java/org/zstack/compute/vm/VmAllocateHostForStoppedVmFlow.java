package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.allocator.AllocateHostReply;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.LastHostPreferredAllocateHostMsg;
import org.zstack.header.allocator.ReturnHostCapacityMsg;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.Map;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostForStoppedVmFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;

    private static final String SUCCESS = VmAllocateHostForStoppedVmFlow.class.getName();

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        LastHostPreferredAllocateHostMsg msg = new LastHostPreferredAllocateHostMsg();
        msg.setVmInstance(spec.getVmInventory());
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum() * spec.getVmInventory().getCpuSpeed());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msg.setVmOperation(spec.getCurrentVmOperation().toString());
        msg.setLastHostUuid(spec.getVmInventory().getLastHostUuid());
        msg.setAllocatorStrategy(HostAllocatorConstant.LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE);
        msg.setL3NetworkUuids(CollectionUtils.transformToList(spec.getL3Networks(), new Function<String, L3NetworkInventory>() {
            @Override
            public String call(L3NetworkInventory arg) {
                return arg.getUuid();
            }
        }));
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocateHostReply areply = (AllocateHostReply) reply;
                    spec.setDestHost(areply.getHost());
                    data.put(SUCCESS, true);
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (data.containsKey(SUCCESS)) {
            HostInventory host = spec.getDestHost();
            ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
            msg.setCpuCapacity(spec.getVmInventory().getCpuNum() * spec.getVmInventory().getCpuSpeed());
            msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
            msg.setHostUuid(host.getUuid());
            msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
            bus.send(msg);
        }
        chain.rollback();
    }
}
