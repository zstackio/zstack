package org.zstack.header.apimediator

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        normalAPIs("org.zstack.header.apimediator.**")
    }
}