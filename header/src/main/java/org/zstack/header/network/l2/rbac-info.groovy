package org.zstack.header.network.l2

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "l2"

            targetResources = [L2NetworkVO.class]

            adminOnlyAPIs("org.zstack.header.network.l2.**")

            normalAPIs(APIUpdateL2NetworkMsg.class.name)
        }

        contributeToRole {
            roleName = "networks"
            normalActionsFromRBAC("l2")
        }
    }
}