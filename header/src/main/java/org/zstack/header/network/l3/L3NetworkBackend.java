package org.zstack.header.network.l3;

import org.zstack.header.message.Message;

public interface L3NetworkBackend {
    void handleMessage(Message msg);
}
