package org.zstack.network.service.lb;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("load-balancer")
                .normalAPIs("org.zstack.network.service.lb.**")
                .targetResources(LoadBalancerVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
    }

    @Override
    public void roles() {
        roleBuilder()
                .name("load-balancer")
                .uuid("cfc42f6e27be4fcc9e93b09356074e7e")
                .permissionsByName("load-balancer", "vip")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
