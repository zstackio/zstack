package org.zstack.header.volume;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "volume";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(VolumeVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("snapshot")
                .actions(APICreateVolumeSnapshotMsg.class)
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("b4368d05a2394f1fb75173683f55456f")
                .permissionBaseOnThis()
                .excludeActions(APICreateVolumeSnapshotMsg.class)
                .build();
    }
}
