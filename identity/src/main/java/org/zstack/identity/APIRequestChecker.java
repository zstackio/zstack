package org.zstack.identity;

import org.zstack.header.message.APIMessage;

public interface APIRequestChecker {
    void check(APIMessage msg);
}
