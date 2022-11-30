package org.zstack.sugonSdnController;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("sugonSdnController")
                .adminOnlyAPIs("org.zstack.sugonSdnController.**")
                .build();
    }

    @Override
    public void contributeToRoles() {
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {
    }
}
