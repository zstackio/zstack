package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class ChangeHostConnectionStateMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String connectionStateEvent;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public String getConnectionStateEvent() {
        return connectionStateEvent;
    }

    public void setConnectionStateEvent(String connectionStateEvent) {
        this.connectionStateEvent = connectionStateEvent;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
