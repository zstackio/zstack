package org.zstack.header.image

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "image"
            normalAPIs("org.zstack.header.image.**")

            targetResources = [ImageVO.class]
        }

        role {
            uuid = "d55b63dc06b14ad1b62448fa6899729b"
            name = "image"
            normalActionsFromRBAC("image")
        }
    }
}