package org.zstack.header.storage.primary

import org.zstack.header.core.StaticInit

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.storage.primary.**")

            normalAPIs(APIQueryPrimaryStorageMsg.class)
        }

        contributeToRole {
            roleName = "other"
            actions(APIQueryPrimaryStorageMsg.class.name)
        }
    }
}