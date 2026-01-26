package org.zstack.plugin.example;

import org.zstack.header.message.Message;

public interface Greeting {
    void handleMessage(Message msg);
}
