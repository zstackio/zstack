package org.zstack.storage.primary.local

import org.zstack.header.core.StaticInit
import org.zstack.header.volume.VolumeVO

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            adminOnlyAPIs("org.zstack.storage.primary.local.**")

            normalAPIs(
                    APILocalStorageGetVolumeMigratableHostsMsg.class.name,
                    APILocalStorageMigrateVolumeMsg.class.name
            )

            targetResources = [VolumeVO.class]

            contributeToRole {
                roleName = "other"
                actions(
                        APILocalStorageGetVolumeMigratableHostsMsg.class.name,
                        APILocalStorageMigrateVolumeMsg.class.name
                )
            }
        }
    }
}