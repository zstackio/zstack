package org.zstack.network.l3;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .normalAPIs("org.zstack.network.l3.**")
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("networks")
                .actions(APIQueryAddressPoolMsg.class)
                .build();
    }

    @Override
    public void roles() {
    }

    @Override
    public void globalReadableResources() {
    }
}
