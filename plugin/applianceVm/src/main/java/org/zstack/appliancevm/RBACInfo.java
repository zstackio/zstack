package org.zstack.appliancevm;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("appliance-vm")
                .targetResources(ApplianceVmVO.class)
                .normalAPIs("org.zstack.appliancevm.**")
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("vrouter")
                .actionsByPermissionName("appliance-vm")
                .build();
    }

    @Override
    public void roles() {
    }

    @Override
    public void globalReadableResources() {
    }
}
