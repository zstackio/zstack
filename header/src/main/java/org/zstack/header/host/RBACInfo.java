package org.zstack.header.host;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("host")
                .adminOnlyAPIs("org.zstack.header.host.**")
                .normalAPIs(APIQueryHostMsg.class)
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
    public void roles() {

    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(HostVO.class)
                .build();
    }
}
