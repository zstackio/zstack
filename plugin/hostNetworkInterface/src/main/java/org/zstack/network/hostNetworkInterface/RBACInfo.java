package org.zstack.network.hostNetworkInterface;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("hostNetwork")
                .adminOnlyAPIs("org.zstack.network.hostNetworkInterface.**")
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("hostNetwork")
                .uuid("4266a77e46cb4e68864899458287941e")
                .permissionsByName("hostNetwork")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}