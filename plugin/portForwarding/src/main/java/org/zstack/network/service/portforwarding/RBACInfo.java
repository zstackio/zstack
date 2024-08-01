package org.zstack.network.service.portforwarding;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "port-forwarding";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(PortForwardingRuleVO.class)
                .communityAvailable()
                .zsvAdvancedAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("62617332af7241dbadf8e0570197d42f")
                .permissionBaseOnThis()
                .permissionsByName("vip")
                .build();
    }
}
