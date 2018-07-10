package org.zstack.header.network.l3;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("l3")
                .normalAPIs("org.zstack.header.network.l3.**")
                .targetResources(L3NetworkVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("networks")
                .uuid("884b0fcc99b04120807e64466fd63336")
                .permissionsByName("l3")
                .build();
    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(L3NetworkVO.class)
                .build();
    }
}
