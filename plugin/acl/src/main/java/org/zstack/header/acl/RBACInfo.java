package org.zstack.header.acl;

import org.zstack.header.identity.rbac.RBACDescription;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-17
 **/
public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "access-control-list";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("4366a67e46cb4e7864899458187961e")
                .permissionBaseOnThis()
                .build();
    }
}
