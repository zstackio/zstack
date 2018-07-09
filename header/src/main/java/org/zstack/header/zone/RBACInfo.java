package org.zstack.header.zone;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("zone")
                .adminOnlyAPIs("org.zstack.header.zone.**")
                .normalAPIs(APIQueryZoneMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actionsByPermissionName("zone")
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(ZoneVO.class)
                .build();
    }
}
