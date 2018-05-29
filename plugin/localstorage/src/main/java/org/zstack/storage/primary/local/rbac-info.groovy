package org.zstack.storage.primary.local

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.storage.primary.local.**")

            normalAPIs(APILocalStorageGetVolumeMigratableHostsMsg.class.name)

            contributeToRole {
                roleName = "other"
                actions(APILocalStorageGetVolumeMigratableHostsMsg.class.name)
            }
        }
    }
}