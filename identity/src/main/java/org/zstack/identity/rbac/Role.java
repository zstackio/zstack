package org.zstack.identity.rbac;

import org.zstack.header.message.Message;

public interface Role {
    void handleMessage(Message msg);
}
