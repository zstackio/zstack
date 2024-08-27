package org.zstack.network.service.vip;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "vip";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(VipVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("cd6ed7e009de2ed6b55d72da2e5526a2")
                .name("vip")
                .permissionBaseOnThis()
                .build();
    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(VipVO.class)
                .build();
    }
}
