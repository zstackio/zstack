package org.zstack.sdnController;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "sdn-controller";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .build();
    }
}
