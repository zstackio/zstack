package org.zstack.header.network.l2;

import org.zstack.header.message.Message;

public interface L2Network {
    void handleMessage(Message msg);

    void deleteHook();
}
