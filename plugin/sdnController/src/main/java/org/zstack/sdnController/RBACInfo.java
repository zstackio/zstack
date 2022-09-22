package org.zstack.sdnController;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("sdnController")
                .adminOnlyAPIs("org.zstack.sdnController.**")
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
