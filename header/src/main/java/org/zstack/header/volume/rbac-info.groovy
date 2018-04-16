package org.zstack.header.volume

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "volume"
            normalAPIs("org.zstack.header.volume.**")
        }

        role {
            name = "volume"
            uuid = "b4368d05a2394f1fb75173683f55456f"
            normalActionsFromRBAC("volume")
        }
    }
}