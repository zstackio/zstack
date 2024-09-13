package org.zstack.header.storage.backup;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.image.ImageVO;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "storage-backup";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(
                        APIQueryBackupStorageMsg.class,
                        APIExportImageFromBackupStorageMsg.class,
                        APIDeleteExportedImageFromBackupStorageMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .targetResources(ImageVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("image")
                .actions(APIDeleteExportedImageFromBackupStorageMsg.class, APIExportImageFromBackupStorageMsg.class)
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(APIQueryBackupStorageMsg.class)
                .build();
    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(BackupStorageVO.class)
                .build();
    }
}
