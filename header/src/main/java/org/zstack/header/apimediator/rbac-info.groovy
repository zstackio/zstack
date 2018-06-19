package org.zstack.header.apimediator

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "portal"
            normalAPIs("org.zstack.header.apimediator.**")
        }

        contributeToRole {
            roleName = "other"
            normalActionsFromRBAC("portal")
        }
    }
}