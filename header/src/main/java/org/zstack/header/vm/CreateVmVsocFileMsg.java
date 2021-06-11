package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

public class CreateVmVsocFileMsg extends NeedReplyMessage {
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    private String vmInstanceUuid;

}