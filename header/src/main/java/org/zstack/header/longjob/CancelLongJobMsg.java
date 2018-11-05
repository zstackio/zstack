package org.zstack.header.longjob;

import org.zstack.header.message.NeedReplyMessage;

public class CancelLongJobMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
