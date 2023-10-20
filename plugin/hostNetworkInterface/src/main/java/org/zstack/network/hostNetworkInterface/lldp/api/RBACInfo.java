package org.zstack.network.hostNetworkInterface.lldp.api;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("lldp")
                .adminOnlyAPIs("org.zstack.network.hostNetworkInterface.lldp.api.**")
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