package org.zstack.header.identity;

import org.zstack.header.message.MessageReply;

public class GetCurrentUsernameReply extends MessageReply {
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
