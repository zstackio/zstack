package org.zstack.storage.ceph;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.storage.ceph.backup.APIQueryCephBackupStorageMsg;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "ceph-storage";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .adminOnlyForAll()
                .normalAPIs(APIQueryCephBackupStorageMsg.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("image")
                .actionsInThisPermission()
                .build();
    }
}
