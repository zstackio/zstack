package org.zstack.header.storage.backup

import org.zstack.header.core.StaticInit
import org.zstack.header.image.ImageVO

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.storage.backup.**")

            normalAPIs(APIQueryBackupStorageMsg.class.name, APIExportImageFromBackupStorageMsg.class.name)

            targetResources = [ImageVO.class]
        }

        contributeToRole {
            roleName = "other"
            actions(APIQueryBackupStorageMsg.class.name, APIExportImageFromBackupStorageMsg.class.name)
        }
    }
}