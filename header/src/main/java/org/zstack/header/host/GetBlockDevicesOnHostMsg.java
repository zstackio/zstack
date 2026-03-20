package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class GetBlockDevicesOnHostMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
