package org.zstack.physicalNetworkInterface;

import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.kvm.KVMHostInventory;

public interface SyncPhysicalNicExtensionPoint {
    Flow createPhysicalNicsPreSyncFlow(KVMHostInventory kvmHostInventory);
    Flow createPhysicalNicsPostSyncFlow(KVMHostInventory kvmHostInventory);
}
