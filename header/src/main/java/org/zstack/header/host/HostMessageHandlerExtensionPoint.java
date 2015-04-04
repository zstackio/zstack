package org.zstack.header.host;

import org.zstack.header.message.Message;

import java.util.List;

public interface HostMessageHandlerExtensionPoint<T> {
    List<String> getMessageNameTheExtensionServed();

    void handleMessage(Message msg, T context);
}
