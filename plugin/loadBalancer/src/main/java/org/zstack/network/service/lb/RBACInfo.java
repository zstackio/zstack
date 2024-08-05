package org.zstack.network.service.lb;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "load-balancer";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(LoadBalancerVO.class)
                .communityAvailable()
                .zsvAdvancedAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("cfc42f6e27be4fcc9e93b09356074e7e")
                .permissionsByName("vip")
                .permissionBaseOnThis()
                .build();
    }
}
