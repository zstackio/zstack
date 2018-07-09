package org.zstack.network.service.flat;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("flat-l3")
                .normalAPIs("org.zstack.network.service.flat.**")
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("networks")
                .actionsByPermissionName("flat-l3")
                .build();
    }

    @Override
    public void roles() {
    }

    @Override
    public void globalReadableResources() {

    }
}
