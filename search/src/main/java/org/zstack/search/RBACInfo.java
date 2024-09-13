package org.zstack.search;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "search";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs(APIRefreshSearchIndexesMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APIRefreshSearchIndexesMsg.class)
                .build();
    }
}
