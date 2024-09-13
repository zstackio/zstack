package org.zstack.sugonSdnController;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "sugon-sdn-controller";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .communityAvailable()
                .build();
    }
}
