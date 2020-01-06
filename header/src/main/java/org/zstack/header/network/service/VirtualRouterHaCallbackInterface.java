package org.zstack.header.network.service;

import org.zstack.header.core.Completion;

import java.util.Map;

public interface VirtualRouterHaCallbackInterface {
    enum Params{
        TaskName,
        OriginRouterUuid,
        PeerRouterUuid,
        Struct,
        Struct1,
    }

    void callBack(String vrUuid, Map<String, Object> data, Completion completion);
}
