package org.zstack.network.l2.vxlan;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.identity.rbac.RBACEntityFormatter;
import org.zstack.header.identity.rbac.RBACEntity;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APIDeleteL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryL2VxlanNetworkPoolMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeMsg;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RBACInfo implements RBACDescription {
    public static final String DELETE_VXLAN_NETWORK_API_NAME = "org.zstack.network.l2.vxlan.vxlanNetwork.APIDeleteVxlanL2Network";

    public RBACEntityFormatter entityFormatter() {
        return new RBACEntityFormatter() {
            @Override
            public Class[] getAPIClasses() {
                return new Class[] {APIDeleteL2NetworkMsg.class};
            }

            @Override
            public RBACEntity format(RBACEntity entity) {
                APIMessage msg = entity.getApiMessage();
                if (msg instanceof APIDeleteL2NetworkMsg) {
                    boolean isVxlan = Q.New(L2NetworkVO.class)
                            .select(L2NetworkVO_.type)
                            .eq(L2NetworkVO_.uuid, ((APIDeleteL2NetworkMsg) msg).getUuid())
                            .findValue() == VxlanNetworkConstant.VXLAN_NETWORK_TYPE;

                    if (isVxlan) {
                        entity.setApiName(DELETE_VXLAN_NETWORK_API_NAME);
                        return entity;
                    }
                }

                return null;
            }
        };
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .name("vxlan")
                .normalAPIs("org.zstack.network.l2.vxlan.vxlanNetwork.**", DELETE_VXLAN_NETWORK_API_NAME)
                .normalAPIs(APIQueryVniRangeMsg.class, APIQueryL2VxlanNetworkPoolMsg.class, APIDeleteL2NetworkMsg.class)
                .adminOnlyAPIs("org.zstack.network.l2.vxlan.vtep.**", "org.zstack.network.l2.vxlan.vxlanNetworkPool.**")
                .targetResources(VxlanNetworkVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("networks")
                .actionsByPermissionName("vxlan")
                .build();
    }

    @Override
    public void roles() {
    }

    @Override
    public void globalReadableResources() {
    }
}
