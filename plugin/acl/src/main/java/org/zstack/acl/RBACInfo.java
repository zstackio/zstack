package org.zstack.acl;

import org.zstack.header.identity.rbac.RBACDescription;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-17
 **/
public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("access-control-list")
                .normalAPIs("org.zstack.header.acl.**")
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("access-control-list")
                .uuid("4366a67e46cb4e7864899458187961e")
                .permissionsByName("access-control-list")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
