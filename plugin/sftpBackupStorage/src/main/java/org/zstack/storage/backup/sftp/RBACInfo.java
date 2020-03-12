package org.zstack.storage.backup.sftp;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("sftp")
                .adminOnlyAPIs("org.zstack.storage.backup.sftp.**")
                .normalAPIs(APIQuerySftpBackupStorageMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("image")
                .actionsByPermissionName("sftp")
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
