package org.zstack.header.image

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        def s = normalAPIs("org.zstack.header.image.**")

        normalRole {
            uuid = "d55b63dc06b14ad1b62448fa6899729b"
            name = "image role"
            allowedActions = s
        }
    }
}