package org.zstack.header.vm

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACInfo.rbac
import static org.zstack.header.identity.rbac.RoleInfo.role

@StaticInit
static void init() {
    def info = rbac {
        normalAPIs("org.zstack.header.vm.**")

        targetResource = VmInstanceVO.class
    }

    role {
        normalRole {
            uuid = "5f93cf6444ec44cc83209744c8c3d7cc"
            name = "vm role"
            allowedActions = info.normalAPIs
        }
    }
}

