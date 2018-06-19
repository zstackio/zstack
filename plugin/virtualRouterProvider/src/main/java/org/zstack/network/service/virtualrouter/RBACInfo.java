package org.zstack.network.service.virtualrouter;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("vrouter")
                .normalAPIs("org.zstack.network.service.virtualrouter.**")
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("vrouter")
                .uuid("74a27f7f461e4601877c2728c52ec9e5")
                .permissionsByName("vrouter", "vip")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
