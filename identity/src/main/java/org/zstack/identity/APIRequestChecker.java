package org.zstack.identity;

import org.zstack.header.message.APIMessage;

public interface APIRequestChecker {
    void check(APIMessage msg);

    default boolean bypass(APIMessage msg) {
        return false;
    }
}
