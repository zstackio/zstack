package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.DeleteVolumeSnapshotSelfOnHypervisorMsg;
import org.zstack.header.host.DeleteVolumeSnapshotSelfOnHypervisorReply;


public interface KVMDeleteVolumeSnapshotSelfExtensionPoint {
    void beforeDeleteVolumeSnapshotSelf(KVMHostInventory host, DeleteVolumeSnapshotSelfOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, Completion completion);

    void afterDeleteVolumeSnapshotSelfVolume(KVMHostInventory host, DeleteVolumeSnapshotSelfOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, DeleteVolumeSnapshotSelfOnHypervisorReply reply, Completion completion);

    void failedToDeleteVolumeSnapshotSelf(KVMHostInventory host, DeleteVolumeSnapshotSelfOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, KVMAgentCommands.BlockCommitVolumeResponse rsp, ErrorCode err);
}
