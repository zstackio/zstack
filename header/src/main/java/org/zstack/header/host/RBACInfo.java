package org.zstack.header.host;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "host";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryHostMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APIQueryHostMsg.class)
                .build();
    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(HostVO.class)
                .build();
    }
}
