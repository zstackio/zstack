package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

public class PingHostReply extends MessageReply {
    private boolean connected;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
