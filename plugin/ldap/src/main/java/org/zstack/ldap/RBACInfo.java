package org.zstack.ldap;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.rest.SDKPackage;

@SDKPackage(packageName = "org.zstack.sdk.identity.ldap")
public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "ldap";
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
