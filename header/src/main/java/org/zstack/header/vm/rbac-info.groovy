package org.zstack.header.vm

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        def s = normalAPIs("org.zstack.header.vm.**")

        normalRole {
            uuid = "5f93cf6444ec44cc83209744c8c3d7cc"
            name = "vm role"
            allowedActions = s
        }
    }
}

