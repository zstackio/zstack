package org.zstack.header.agent.versioncontrol;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.header.agent.**")
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
