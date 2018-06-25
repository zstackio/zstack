package org.zstack.header.network.l2;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("l2")
                .targetResources(L2NetworkVO.class)
                .adminOnlyAPIs("org.zstack.header.network.l2.**")
                .normalAPIs(APIUpdateL2NetworkMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("networks")
                .actionsByPermissionName("l2")
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(L2NetworkVO.class)
                .build();
    }
}
