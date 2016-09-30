package org.zstack.header.vm;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VmInstanceDeletionMsg extends DeletionMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String deletionPolicy;

    public String getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(String deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
