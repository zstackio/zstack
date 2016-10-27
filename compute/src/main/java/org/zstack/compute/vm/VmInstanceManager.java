package org.zstack.compute.vm;

import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.vm.VmAccountPerference;
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

    FlowChain getAttachIsoWorkFlowChain(VmInstanceInventory inv);

    FlowChain getDetachIsoWorkFlowChain(VmInstanceInventory inv);

    FlowChain getExpungeVmWorkFlowChain(VmInstanceInventory inv);

    FlowChain getChangeVmPasswordWorkFlowChain();

    FlowChain getSetVmRootPasswordWorkFlowChain();

    VmInstanceFactory getVmInstanceFactory(VmInstanceType vmType);
}
