package org.zstack.header.core.external.service;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs(
                        APIGetExternalServicesMsg.class,
                        APIReloadExternalServiceMsg.class
                ).build();
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
