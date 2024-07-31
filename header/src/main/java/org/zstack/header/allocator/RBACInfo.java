package org.zstack.header.allocator;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("host-allocator")
                .adminOnlyAPIs("org.zstack.header.allocator.**")
                .normalAPIs(APIGetCpuMemoryCapacityMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APIGetCpuMemoryCapacityMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
