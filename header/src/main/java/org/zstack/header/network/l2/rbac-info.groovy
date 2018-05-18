package org.zstack.header.network.l2

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "l2"

            adminOnlyAPIs("org.zstack.header.network.l2.**")

            normalAPIs(APIUpdateL2NetworkMsg.class.name)
        }

        contributeToRole {
            roleName = "networks"
            normalActionsFromRBAC("l2")
        }
    }
}