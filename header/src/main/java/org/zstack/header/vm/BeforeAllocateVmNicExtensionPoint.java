package org.zstack.header.vm;

import org.zstack.header.core.Completion;

/**
 * Extension point called after a VmNic is persisted but before it is fully configured
 * in VmAllocateNicFlow. The VmNicVO (and its ResourceVO) already exists in the database,
 * so implementations may safely create SystemTags referencing the NIC UUID.
 * If the implementation fails, the NIC will be cleaned up during flow rollback.
 * Use case: create SDN segment ports and save port-UUID system tags for the NIC.
 */
public interface BeforeAllocateVmNicExtensionPoint {
    void beforeAllocateVmNic(VmNicInventory nic, VmInstanceSpec spec, Completion completion);
}
