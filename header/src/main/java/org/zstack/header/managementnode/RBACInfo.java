package org.zstack.header.managementnode;

import org.zstack.header.identity.rbac.RBACDescription;

/**
 * Created by kayo on 2018/7/10.
 */
public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "management-node";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }
}
