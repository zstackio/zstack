package org.zstack.header.network.service;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.header.network.service.**")
                .normalAPIs(
                        APIAttachNetworkServiceToL3NetworkMsg.class,
                        APIQueryNetworkServiceProviderMsg.class
                ).build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("networks")
                .actions(APIQueryNetworkServiceProviderMsg.class, APIAttachNetworkServiceToL3NetworkMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
