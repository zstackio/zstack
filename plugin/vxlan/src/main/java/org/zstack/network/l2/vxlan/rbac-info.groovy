package org.zstack.network.l2.vxlan

import org.zstack.header.core.StaticInit
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolMsg
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeMsg

import static org.zstack.header.identity.rbac.RBAC.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "vxlan"
            normalAPIs("org.zstack.network.l2.vxlan.vxlanNetwork.**")
            normalAPIs(
                    APIQueryVniRangeMsg.class.name,
                    APIQueryL2VxlanNetworkPoolMsg.class.name
            )

            adminOnlyAPIs("org.zstack.network.l2.vxlan.vtep.**")
            adminOnlyAPIs("org.zstack.network.l2.vxlan.vxlanNetworkPool.**")
        }

        contributeToRole {
            roleName = "l3"
            normalActionsFromRBAC("vxlan")
        }
    }
}