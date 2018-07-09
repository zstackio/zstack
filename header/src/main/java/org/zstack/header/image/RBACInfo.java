package org.zstack.header.image;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("image")
                .normalAPIs("org.zstack.header.image.**")
                .targetResources(ImageVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("image")
                .uuid("d55b63dc06b14ad1b62448fa6899729b")
                .permissionsByName("image")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
