package org.zstack.core.config;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "global-config";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIQueryGlobalConfigMsg.class,
                        APIGetGlobalConfigOptionsMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(
                        APIQueryGlobalConfigMsg.class,
                        APIGetGlobalConfigOptionsMsg.class)
                .build();
    }
}
