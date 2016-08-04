package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
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

        AllocateHostMsg amsg;

        DesignatedAllocateHostMsg msg = new DesignatedAllocateHostMsg();
        msg.setVmInstance(spec.getVmInventory());
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        msg.setVmOperation(spec.getCurrentVmOperation().toString());
        if ((spec.getRequiredClusterUuid() != null &&
                !spec.getRequiredClusterUuid().equals(msg.getVmInstance().getClusterUuid()))
                || spec.getRequiredHostUuid() != null) {
            msg.setAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);
            msg.setHostUuid(spec.getRequiredHostUuid());
        } else {
            msg.setAllocatorStrategy(HostAllocatorConstant.LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE);
        }
        msg.setL3NetworkUuids(CollectionUtils.transformToList(spec.getL3Networks(), new Function<String, L3NetworkInventory>() {
            @Override
            public String call(L3NetworkInventory arg) {
                return arg.getUuid();
            }
        }));
        msg.setClusterUuid(spec.getRequiredClusterUuid());
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));

        amsg = msg;

        bus.send(amsg, new CloudBusCallBack(chain) {
            @Transactional
            private void setVmHostUuid(String huuid) {
                VmInstanceVO vo = dbf.getEntityManager().find(VmInstanceVO.class, spec.getVmInventory().getUuid());
                vo.setHostUuid(huuid);
                dbf.getEntityManager().merge(vo);
            }

            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocateHostReply areply = (AllocateHostReply) reply;
                    spec.setDestHost(areply.getHost());
                    setVmHostUuid(areply.getHost().getUuid());
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
            VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
            vm.setHostUuid(null);
            dbf.update(vm);

            HostInventory host = spec.getDestHost();
            ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
            msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
            msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
            msg.setHostUuid(host.getUuid());
            msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
            bus.send(msg);
        }
        chain.rollback();
    }
}
