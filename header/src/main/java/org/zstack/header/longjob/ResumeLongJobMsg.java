package org.zstack.header.longjob;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2020/4/23.
 */
public class ResumeLongJobMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
