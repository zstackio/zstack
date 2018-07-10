package org.zstack.network.securitygroup;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("security-group")
                .normalAPIs("org.zstack.network.securitygroup.**")
                .targetResources(SecurityGroupVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("security-group")
                .uuid("4266a67e46cb4e68864899458187941e")
                .permissionsByName("security-group")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
