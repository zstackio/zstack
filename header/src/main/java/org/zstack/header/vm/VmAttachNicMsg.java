package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class VmAttachNicMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String l3NetworkUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
}
