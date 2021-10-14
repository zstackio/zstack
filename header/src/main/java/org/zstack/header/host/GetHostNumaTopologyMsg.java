package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class GetHostNumaTopologyMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    private String hostUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
