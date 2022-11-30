package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class GetVmVirtualizerVersionMsg extends NeedReplyMessage implements HostMessage {
    private String vmInstanceUuid;
    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
