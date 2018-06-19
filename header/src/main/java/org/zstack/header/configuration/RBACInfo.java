package org.zstack.header.configuration;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("configuration")
                .adminOnlyAPIs("org.zstack.header.configuration.**")
                .targetResources(InstanceOfferingVO.class, DiskOfferingVO.class)
                .normalAPIs(APIQueryDiskOfferingMsg.class, APIQueryInstanceOfferingMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("configuration")
                .uuid("067c4dc358e847aba47903ca4fb1c41c")
                .permissionsByName("configuration")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
