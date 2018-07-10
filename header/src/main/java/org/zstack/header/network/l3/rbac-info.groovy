package org.zstack.header.network.l3

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "l3"
            normalAPIs("org.zstack.header.network.l3.**")

            targetResources = [L3NetworkVO.class]
        }

        role {
            name = "networks"
            uuid = "884b0fcc99b04120807e64466fd63336"
            normalActionsFromRBAC("l3")
        }
    }
}