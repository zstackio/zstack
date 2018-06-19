package org.zstack.network.service.portforwarding;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("port-forwarding")
                .normalAPIs("org.zstack.network.service.portforwarding.**")
                .targetResources(PortForwardingRuleVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("port-forwarding")
                .uuid("62617332af7241dbadf8e0570197d42f")
                .permissionsByName("port-forwarding", "vip")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
