package org.zstack.core.cloudbus;

import org.zstack.header.message.Message;

import java.util.List;

/**
 */
public interface ReplyMessagePreSendingExtensionPoint {
    List<Class> getReplyMessageClassForPreSendingExtensionPoint();

    void marshalReplyMessageBeforeSending(Message msg);
}
