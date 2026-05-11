package org.zstack.header.server;

import org.zstack.header.message.NeedReplyMessage;

public class PingPhysicalServerMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
