package org.zstack.network.service.virtualrouter

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "vrouter"
            normalAPIs("org.zstack.network.service.virtualrouter.**")

            adminOnlyAPIs(APICreateVirtualRouterOfferingMsg.class.name)
        }

        role {
            name = "vrouter"
            uuid = "74a27f7f461e4601877c2728c52ec9e5"
            normalActionsFromRBAC("vrouter", "vip")
        }
    }
}