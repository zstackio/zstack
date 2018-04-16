package org.zstack.network.service.eip

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "eip"
            normalAPIs("org.zstack.network.service.eip.**")
        }

        role {
            uuid = "ecae3a96ee1b47c2aa2baee1e1110550"
            name = "eip"
            normalActionsFromRBAC("vip", "eip")
        }
    }
}