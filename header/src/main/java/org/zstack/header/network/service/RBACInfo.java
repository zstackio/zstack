package org.zstack.header.network.service;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "network-service";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIAttachNetworkServiceToL3NetworkMsg.class,
                        APIQueryNetworkServiceProviderMsg.class
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
                .actions(APIQueryNetworkServiceProviderMsg.class, APIAttachNetworkServiceToL3NetworkMsg.class)
                .build();
    }
}
