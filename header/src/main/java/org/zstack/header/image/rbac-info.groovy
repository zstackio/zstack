package org.zstack.header.image

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac
import static org.zstack.header.identity.rbac.RoleInfo.role

@StaticInit
static void init() {
    def info = rbac {
        normalAPIs("org.zstack.header.image.**")
    }

    role {
        normalRole {
            uuid = "d55b63dc06b14ad1b62448fa6899729b"
            name = "image role"
            allowedActions = info.normalAPIs
        }
    }
}