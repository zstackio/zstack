package org.zstack.header.storage.backup;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.image.ImageVO;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.header.storage.backup.**")
                .normalAPIs(
                        APIQueryBackupStorageMsg.class,
                        APIExportImageFromBackupStorageMsg.class,
                        APIDeleteExportedImageFromBackupStorageMsg.class
                )
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
                .roleName("other")
                .actions(APIQueryBackupStorageMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {
        globalReadableResourceBuilder()
                .resources(BackupStorageVO.class)
                .build();
    }
}
