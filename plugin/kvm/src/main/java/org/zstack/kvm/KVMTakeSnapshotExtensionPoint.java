package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.TakeSnapshotOnHypervisorMsg;


public interface KVMTakeSnapshotExtensionPoint {
    void beforeTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion);

    void afterTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg);

    void afterTakeSnapshotFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, ErrorCode err);
}
