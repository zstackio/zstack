package org.zstack.identity;

import org.zstack.header.identity.rbac.RBACEntity;

public interface APIRequestChecker {
    void check(RBACEntity msg);

    default boolean bypass(RBACEntity msg) {
        return false;
    }
}
