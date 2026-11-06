package org.zstack.kvm;

import org.zstack.header.message.NeedReplyMessage;

public class KVMVmFirmwareFlashCleanupMsg extends NeedReplyMessage {
    private String vmUuid;
    private String hostUuid;
    private String generation;
    private String jobUuid;

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public String getGeneration() {
        return generation;
    }

    public void setGeneration(String generation) {
        this.generation = generation;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
