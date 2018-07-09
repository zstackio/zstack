package org.zstack.network.service.eip;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("eip")
                .normalAPIs("org.zstack.network.service.eip.**")
                .targetResources(EipVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("ecae3a96ee1b47c2aa2baee1e1110550")
                .name("eip")
                .permissionsByName("vip", "eip")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
