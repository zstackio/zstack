package org.zstack.header.image

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        def s = normalAPIs("org.zstack.header.image.**")

        normalRole {
            uuid = "0ce6f173cdab42968ec7bf98bb34a41e"
            name = "image role"
            allowedActions = s
        }
    }
}