package org.zstack.header.network.l2;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "l2";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(L2NetworkVO.class)
                .adminOnlyForAll()
                .normalAPIs(
                        APIUpdateL2NetworkMsg.class,
                        APIGetL2NetworkTypesMsg.class,
                        APIGetVSwitchTypesMsg.class,
                        APIQueryL2NetworkMsg.class,
                        APIQueryL2VlanNetworkMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("networks")
                .actionsInThisPermission()
                .build();
    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(L2NetworkVO.class)
                .build();
    }
}
