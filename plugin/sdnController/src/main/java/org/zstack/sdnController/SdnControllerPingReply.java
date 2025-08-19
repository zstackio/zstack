package org.zstack.sdnController;

import org.zstack.header.message.MessageReply;

/**
 * Created by shixin.ruan on 06/26/2025.
 */
public class SdnControllerPingReply extends MessageReply {
    private boolean doReconnect = false;

    public boolean getDoReconnect() {
        return doReconnect;
    }

    public void setDoReconnect(boolean doReconnect) {
        this.doReconnect = doReconnect;
    }
}
