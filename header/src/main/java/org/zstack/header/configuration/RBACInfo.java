package org.zstack.header.configuration;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "configuration";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .targetResources(InstanceOfferingVO.class, DiskOfferingVO.class)
                .normalAPIs(APIQueryDiskOfferingMsg.class, APIQueryInstanceOfferingMsg.class, APICreateDiskOfferingMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("067c4dc358e847aba47903ca4fb1c41c")
                .permissionBaseOnThis()
                .build();
    }
}
