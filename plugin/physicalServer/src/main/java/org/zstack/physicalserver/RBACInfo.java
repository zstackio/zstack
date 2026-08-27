package org.zstack.physicalserver;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("physicalServer")
                .adminOnlyAPIs("org.zstack.physicalserver.**")
                .build();
    }

    @Override
    public void contributeToRoles() {
    }

    @Override
    public void roles() {
        roleBuilder()
                .name("physicalServer")
                .uuid("f8d781036ed84b69a9737ec6d78e65e1")
                .permissionsByName("physicalServer")
                .build();
    }

    @Override
    public void globalReadableResources() {
    }
}
