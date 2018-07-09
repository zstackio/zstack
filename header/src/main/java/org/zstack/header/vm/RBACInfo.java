package org.zstack.header.vm;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("vm")
                .normalAPIs("org.zstack.header.vm.**")
                .targetResources(VmInstanceVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("5f93cf6444ec44cc83209744c8c3d7cc")
                .name("vm")
                .permissionsByName("vm")
                .build();

        roleBuilder()
                .uuid("d6b79564f9b641a4b8bb85ea249151c2")
                .name("vm-operation-without-create-permission")
                .permissionsByName("vm")
                .excludeActions(APICreateVmInstanceMsg.class)
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
