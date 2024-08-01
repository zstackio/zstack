package org.zstack.storage.ceph;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.storage.ceph.backup.APIQueryCephBackupStorageMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("ceph-storage")
                .adminOnlyAPIs("org.zstack.storage.ceph.**")
                .normalAPIs(APIQueryCephBackupStorageMsg.class)
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("image")
                .actions(APIQueryCephBackupStorageMsg.class)
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
