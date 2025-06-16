package org.zstack.sdnController;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("sdnController")
                .adminOnlyAPIs("org.zstack.sdnController.**")
                .build();
    }

    @Override
    public void contributeToRoles() {
    }

    @Override
    public void roles() {
        roleBuilder()
                .name("sdnController")
                .uuid("4266a67e46cb4e68864899458287941e")
                .permissionsByName("sdnController")
                .build();
    }

    @Override
    public void globalReadableResources() {
    }
}
