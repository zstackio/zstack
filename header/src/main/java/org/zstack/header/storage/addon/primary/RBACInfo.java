package org.zstack.header.storage.addon.primary;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "external-primary-storage";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.header.storage.addon.primary.**")
                .communityAvailable()
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
        globalReadableResourceBuilder()
                .resources(ExternalPrimaryStorageVO.class)
                .build();
    }
}
