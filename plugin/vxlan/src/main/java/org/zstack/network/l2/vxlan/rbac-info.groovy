package org.zstack.network.l2.vxlan

import org.zstack.core.db.SQLBatchWithReturn
import org.zstack.header.core.StaticInit
import org.zstack.header.network.l2.APIDeleteL2NetworkMsg
import org.zstack.header.network.l2.L2NetworkVO
import org.zstack.header.network.l2.L2NetworkVO_
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO
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
                    APIQueryL2VxlanNetworkPoolMsg.class.name,
            )

            targetResources = [VxlanNetworkVO.class]

            adminOnlyAPIs("org.zstack.network.l2.vxlan.vtep.**")
            adminOnlyAPIs("org.zstack.network.l2.vxlan.vxlanNetworkPool.**")

            registerAPIPermissionChecker(APIDeleteL2NetworkMsg.class, true) { APIDeleteL2NetworkMsg msg ->
                return new SQLBatchWithReturn<Boolean>() {
                    @Override
                    protected Boolean scripts() {
                        boolean isVxlan = q(L2NetworkVO.class)
                                .select(L2NetworkVO_.type)
                                .eq(L2NetworkVO_.uuid, msg.getUuid())
                                .findValue() == VxlanNetworkConstant.VXLAN_NETWORK_TYPE

                        //FIXME: hack to fix http://jira.zstack.io/browse/ZSTAC-12946
                        // return isVxlan ? true : null
                        return isVxlan
                    }
                }.execute()
            }
        }

        contributeToRole {
            roleName = "networks"
            normalActionsFromRBAC("vxlan")
            //FIXME: hack to fix http://jira.zstack.io/browse/ZSTAC-12946
            actions(APIDeleteL2NetworkMsg.class.name)
        }
    }
}