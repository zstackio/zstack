package org.zstack.header.identity;

import org.zstack.header.message.NeedReplyMessage;

public class GetCurrentUsernameMsg extends NeedReplyMessage {
    private String sessionUuid;

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }
}
