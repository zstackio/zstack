package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.ReturnHostCapacityMsg;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReturnHostFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    
    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        if (spec.getVmInventory().getHostUuid() == null) {
            // the vm failed because no host available at that time
            // no need to return host
            chain.next();
            return;
        }

        HostVO hvo = dbf.findByUuid(spec.getVmInventory().getHostUuid(), HostVO.class);
        HostInventory hinv = HostInventory.valueOf(hvo);

        ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
        msg.setHost(hinv);
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum()*spec.getVmInventory().getCpuSpeed());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        bus.send(msg);
        chain.next();
    }
}
