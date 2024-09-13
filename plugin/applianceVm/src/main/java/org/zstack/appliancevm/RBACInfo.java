package org.zstack.appliancevm;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "appliance-vm";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(ApplianceVmVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("vrouter")
                .actionsInThisPermission()
                .build();
    }
}
