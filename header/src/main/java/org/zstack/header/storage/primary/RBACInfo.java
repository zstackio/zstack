package org.zstack.header.storage.primary;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "primary-storage";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryPrimaryStorageMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        contributeNormalApiToOtherRole();
    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(PrimaryStorageVO.class)
                .build();
    }
}
