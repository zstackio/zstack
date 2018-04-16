package org.zstack.header.storage.snapshot

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "snapshot"
            normalAPIs("org.zstack.header.storage.snapshot.**")
        }

        role {
            name = "snapshot"
            uuid = "a91363c6b4ba4e58966d17a4257668cd"
            normalActionsFromRBAC("snapshot")
        }
    }
}