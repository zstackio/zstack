package org.zstack.header.vm;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VmInstanceDeletionMsg extends DeletionMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
