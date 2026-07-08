package org.zstack.header.storage.addon.backup;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.storage.backup.BackupStorageVO;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "external-backup-storage";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyAPIs("org.zstack.header.storage.addon.backup.**")
                .communityAvailable()
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
        globalReadableResourceBuilder()
                .resources(BackupStorageVO.class)
                .build();
    }
}
