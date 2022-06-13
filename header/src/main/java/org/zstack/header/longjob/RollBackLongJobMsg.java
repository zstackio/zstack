package org.zstack.header.longjob;

import org.zstack.header.message.NeedReplyMessage;

public class RollBackLongJobMsg extends NeedReplyMessage implements LongJobMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLongJobUuid() {
        return uuid;
    }
}
