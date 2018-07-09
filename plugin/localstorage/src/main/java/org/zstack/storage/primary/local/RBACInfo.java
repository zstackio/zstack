package org.zstack.storage.primary.local;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.volume.VolumeVO;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.storage.primary.local.**")
                .normalAPIs(APILocalStorageGetVolumeMigratableHostsMsg.class, APILocalStorageMigrateVolumeMsg.class)
                .targetResources(VolumeVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actions(APILocalStorageGetVolumeMigratableHostsMsg.class, APILocalStorageMigrateVolumeMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
