package org.zstack.identity;

import org.zstack.identity.rbac.datatype.RBACEntity;

public interface APIRequestChecker {
    void check(RBACEntity msg);

    default boolean bypass(RBACEntity msg) {
        return false;
    }
}
