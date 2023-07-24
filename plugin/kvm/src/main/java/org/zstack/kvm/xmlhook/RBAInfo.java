package org.zstack.kvm.xmlhook;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBAInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.kvm.xmlhook.**")
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

