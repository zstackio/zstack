package org.zstack.header.allocator;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "host-allocator";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIGetCpuMemoryCapacityMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        contributeNormalApiToOtherRole();
    }
}
