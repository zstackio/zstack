package org.zstack.network.l2.vxlan;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.network.l2.APIDeleteL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.APIDeleteVxlanL2Network;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeMsg;

import java.util.Collections;

import static org.zstack.utils.CollectionDSL.list;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "vxlan";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs(
                        APIQueryVniRangeMsg.class,
                        APIQueryL2VxlanNetworkPoolMsg.class,
                        APIDeleteL2NetworkMsg.class,
                        APIDeleteVxlanL2Network.class
                )
                .normalAPIs("org.zstack.network.l2.vxlan.vxlanNetwork.**")
                .adminOnlyForAll()
                .targetResources(VxlanNetworkVO.class)
                .communityAvailable()
                .zsvAdvancedAvailable()
                .build();

        expandedPermission(APIDeleteL2NetworkMsg.class, api -> {
            boolean vxlan = Q.New(L2NetworkVO.class)
                    .eq(L2NetworkVO_.uuid, api.getUuid())
                    .eq(L2NetworkVO_.type, VxlanNetworkConstant.VXLAN_NETWORK_TYPE)
                    .isExists();
            if (vxlan) {
                APIDeleteVxlanL2Network expendMsg = new APIDeleteVxlanL2Network();
                expendMsg.setUuid(api.getUuid());
                return list(expendMsg);
            }

            return Collections.emptyList();
        });
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("networks")
                .actionsInThisPermission()
                .build();
    }
}
