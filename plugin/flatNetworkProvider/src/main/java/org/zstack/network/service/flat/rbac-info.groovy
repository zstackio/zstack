package org.zstack.network.service.flat

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "flat-l3"
            normalAPIs("org.zstack.network.service.flat.**")
        }

        contributeToRole {
            roleName = "networks"

            normalActionsFromRBAC("flat-l3")
        }
    }
}