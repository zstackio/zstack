package org.zstack.header.network.l3;

import org.zstack.header.message.Message;

import java.util.List;

public interface L3NetworkBaseExtensionFactory {
    L3Network getL3Network(L3NetworkVO vo);

    List<Class<? extends Message>> getMessageClasses();
}
