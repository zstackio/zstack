package org.zstack.core.notification

import org.zstack.header.core.StaticInit
import org.zstack.header.storage.backup.APIQueryBackupStorageMsg
import org.zstack.header.vo.APIGetResourceNamesMsg

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        adminOnlyAPIs("org.zstack.core.notification.**")
    }
}