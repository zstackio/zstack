package org.zstack.kvm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class KVMHotPlugVmShmemMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmUuid;
    private KVMAgentCommands.VmShmemDevice shmem;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public KVMAgentCommands.VmShmemDevice getShmem() {
        return shmem;
    }

    public void setShmem(KVMAgentCommands.VmShmemDevice shmem) {
        this.shmem = shmem;
    }
}
