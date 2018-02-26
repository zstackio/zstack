package org.zstack.header.core.progress

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        normalAPIs(
                APIGetTaskProgressMsg.class.name,
        )
    }
}

