package org.zstack.compute.vm;

import org.zstack.core.workflow.FlowChain;
import org.zstack.header.Service;
import org.zstack.header.vm.VmInstanceFactory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceType;

public interface VmInstanceManager {
    FlowChain getCreateVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getStopVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getRebootVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getStartVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getDestroyVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getMigrateVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getAttachUninstantiatedVolumeWorkFlowChain(VmInstanceInventory inv);

    VmInstanceFactory getVmInstanceFactory(VmInstanceType vmType);
}
