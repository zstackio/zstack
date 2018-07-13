package org.zstack.header.storage.backup

import org.zstack.header.core.StaticInit
import org.zstack.header.image.ImageVO

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.header.storage.backup.**")

            normalAPIs(
                    APIQueryBackupStorageMsg.class.name,
                    APIExportImageFromBackupStorageMsg.class.name,
                    APIDeleteExportedImageFromBackupStorageMsg.class.name
            )

            targetResources = [ImageVO.class]
        }

        contributeToRole {
            roleName = "image"
            actions(APIDeleteExportedImageFromBackupStorageMsg.class.name, APIExportImageFromBackupStorageMsg.class.name)
        }

        contributeToRole {
            roleName = "other"
            actions(APIQueryBackupStorageMsg.class.name)
        }
    }
}