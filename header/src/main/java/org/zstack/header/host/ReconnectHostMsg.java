package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class ReconnectHostMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private boolean skipIfHostConnected;

    public boolean isSkipIfHostConnected() {
        return skipIfHostConnected;
    }

    public void setSkipIfHostConnected(boolean skipIfHostConnected) {
        this.skipIfHostConnected = skipIfHostConnected;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
