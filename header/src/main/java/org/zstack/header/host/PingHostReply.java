package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

public class PingHostReply extends MessageReply {
    private boolean connected;
    private String currentHostStatus;

    public String getCurrentHostStatus() {
        return currentHostStatus;
    }

    public void setCurrentHostStatus(String currentHostStatus) {
        this.currentHostStatus = currentHostStatus;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
