package org.zstack.network.service.vip;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("vip")
                .normalAPIs("org.zstack.network.service.vip.**")
                .targetResources(VipVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(VipVO.class)
                .build();
    }
}
