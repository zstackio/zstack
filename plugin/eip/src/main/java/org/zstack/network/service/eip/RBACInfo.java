package org.zstack.network.service.eip;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "eip";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(EipVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("ecae3a96ee1b47c2aa2baee1e1110550")
                .permissionBaseOnThis()
                .permissionsByName("vip")
                .build();
    }
}
