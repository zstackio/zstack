package org.zstack.network.l2.vxlan

import org.zstack.header.core.StaticInit
import org.zstack.header.network.l2.APIDeleteL2NetworkMsg
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolMsg
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeMsg

import static org.zstack.header.identity.rbac.RBACGroovy.rbac

@StaticInit
static void init() {
    rbac {
        permissions {
            name = "vxlan"
            normalAPIs("org.zstack.network.l2.vxlan.vxlanNetwork.**")
            normalAPIs(
                    APIQueryVniRangeMsg.class.name,
                    APIQueryL2VxlanNetworkPoolMsg.class.name,
            )
            normalAPIs(APIDeleteL2NetworkMsg.class.name, RBACInfo.DELETE_VXLAN_NETWORK_API_NAME)

            adminOnlyAPIs("org.zstack.network.l2.vxlan.vtep.**", "org.zstack.network.l2.vxlan.vxlanNetworkPool.**")

            targetResources = [VxlanNetworkVO.class]
        }

        contributeToRole {
            roleName = "networks"
            normalActionsFromRBAC("vxlan")
        }
    }
}