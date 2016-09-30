package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 6/29/2015.
 */
public class PingVirtualRouterVmReply extends MessageReply {
    private boolean connected;
    private boolean doReconnect;

    public boolean isDoReconnect() {
        return doReconnect;
    }

    public void setDoReconnect(boolean doReconnect) {
        this.doReconnect = doReconnect;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
