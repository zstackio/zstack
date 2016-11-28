package org.zstack.header.host;

import org.zstack.header.message.Message;

public interface Host {
    void handleMessage(Message msg);
}
