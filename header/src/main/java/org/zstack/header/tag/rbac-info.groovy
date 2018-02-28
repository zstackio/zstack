package org.zstack.header.tag

import org.zstack.header.core.StaticInit
import org.zstack.header.storage.backup.APIQueryBackupStorageMsg

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        normalAPIs("org.zstack.header.tag.**")
    }
}