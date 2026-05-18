package org.zstack.zcenter.accounts;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.rest.SDKPackage;

@SDKPackage(packageName = "org.zstack.sdk.agents.zcenter.accounts")
public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "zcenter-accounts";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvProAvailable()
                .build();
    }
}
