package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.header.core.StaticInit
import org.zstack.header.storage.backup.APIQueryBackupStorageMsg
import org.zstack.header.vo.APIGetResourceNamesMsg

import static org.zstack.header.identity.rbac.RBACInfo.rbac

@StaticInit
static void init() {
    rbac {
        adminOnlyAPIs("org.zstack.network.l2.vxlan.vxlanNetworkPool.**")

        normalAPIs(
                APIQueryVniRangeMsg.class.name,
                APIQueryL2VxlanNetworkPoolMsg.class.name
        )
    }
}