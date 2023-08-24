package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.BlockCommitVolumeOnHypervisorMsg;
import org.zstack.header.host.BlockCommitVolumeOnHypervisorReply;
import org.zstack.header.host.TakeSnapshotOnHypervisorMsg;


public interface KVMBlockCommitExtensionPoint {
    void beforeVolumeCommit(KVMHostInventory host, BlockCommitVolumeOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, Completion completion);

    void afterVolumeCommit(KVMHostInventory host, BlockCommitVolumeOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, BlockCommitVolumeOnHypervisorReply reply, Completion completion);

    void afterVolumeCommitFailed(KVMHostInventory host, BlockCommitVolumeOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, KVMAgentCommands.BlockCommitVolumeResponse rsp, ErrorCode err);
}
