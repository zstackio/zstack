package org.zstack.header.storage.snapshot

import org.zstack.header.core.StaticInit
import org.zstack.header.volume.VolumeVO

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "snapshot"
            normalAPIs("org.zstack.header.storage.snapshot.**")

            targetResources = [VolumeSnapshotVO.class, VolumeSnapshotTreeVO.class, VolumeVO.class]
        }

        role {
            name = "snapshot"
            uuid = "a91363c6b4ba4e58966d17a4257668cd"
            normalActionsFromRBAC("snapshot")
        }
    }
}