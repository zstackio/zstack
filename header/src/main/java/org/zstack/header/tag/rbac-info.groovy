package org.zstack.header.tag

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "tag"
            normalAPIs("org.zstack.header.tag.**")
        }

        contributeToRole {
            roleName = "other"
            normalActionsFromRBAC("tag")
        }
    }
}