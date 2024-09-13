package org.zstack.storage.primary.local;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.volume.VolumeVO;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "local-storage";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APILocalStorageGetVolumeMigratableHostsMsg.class, APILocalStorageMigrateVolumeMsg.class,
                        APIQueryLocalStorageResourceRefMsg.class)
                .targetResources(VolumeVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APILocalStorageGetVolumeMigratableHostsMsg.class, APILocalStorageMigrateVolumeMsg.class,
                        APIQueryLocalStorageResourceRefMsg.class)
                .build();
    }
}
