package org.zstack.sdnController;

import org.zstack.header.message.MessageReply;

/**
 * Created by shixin.ruan on 06/26/2025.
 */
public class SdnControllerPingReply extends MessageReply {
    Boolean doReconnect = false;

    public Boolean getDoReconnect() {
        return doReconnect;
    }

    public void setDoReconnect(Boolean doReconnect) {
        this.doReconnect = doReconnect;
    }
}
