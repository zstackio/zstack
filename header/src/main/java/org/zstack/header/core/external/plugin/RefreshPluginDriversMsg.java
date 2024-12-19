package org.zstack.header.core.external.plugin;

import org.zstack.header.message.NeedReplyMessage;

public class RefreshPluginDriversMsg extends NeedReplyMessage {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
