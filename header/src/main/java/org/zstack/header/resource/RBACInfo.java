package org.zstack.header.resource;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("resource-source")
                .adminOnlyAPIs(APIQueryResourceSourceRefMsg.class)
                .targetResources(ResourceSourceRefVO.class)
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
