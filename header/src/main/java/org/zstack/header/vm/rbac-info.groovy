package org.zstack.header.vm

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "vm"
            normalAPIs("org.zstack.header.vm.**")

            targetResource = VmInstanceVO.class
        }

        role {
            uuid = "5f93cf6444ec44cc83209744c8c3d7cc"
            name = "vm"
            normalActionsFromRBAC("vm")
        }
    }
}

