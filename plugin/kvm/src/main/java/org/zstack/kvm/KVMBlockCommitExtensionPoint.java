package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.CommitVolumeOnHypervisorMsg;
import org.zstack.header.host.CommitVolumeOnHypervisorReply;


public interface KVMBlockCommitExtensionPoint {
    void beforeCommitVolume(KVMHostInventory host, CommitVolumeOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, Completion completion);

    void afterCommitVolume(KVMHostInventory host, CommitVolumeOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, CommitVolumeOnHypervisorReply reply, Completion completion);

    void failedToCommitVolume(KVMHostInventory host, CommitVolumeOnHypervisorMsg msg, KVMAgentCommands.BlockCommitVolumeCmd cmd, KVMAgentCommands.BlockCommitVolumeResponse rsp, ErrorCode err);
}
