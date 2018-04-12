package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.TakeSnapshotOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;


public interface KVMTakeSnapshotExtensionPoint {
    KVMAgentCommands.TakeSnapshotCmd beforeTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd);

    void afterTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg);

    void afterTakeSnapshotFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, ErrorCode err);
}
