package org.zstack.header.image;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "image";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(ImageVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("d55b63dc06b14ad1b62448fa6899729b")
                .permissionBaseOnThis()
                .build();
    }
}
