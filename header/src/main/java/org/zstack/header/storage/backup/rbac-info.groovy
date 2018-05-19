package org.zstack.header.storage.backup

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.storage.backup.**")

            normalAPIs(APIQueryBackupStorageMsg.class.name)
        }

        contributeToRole {
            roleName = "other"
            actions(APIQueryBackupStorageMsg.class.name)
        }
    }
}