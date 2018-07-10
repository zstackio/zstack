package org.zstack.header.volume;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("volume")
                .normalAPIs("org.zstack.header.volume.**")
                .targetResources(VolumeVO.class)
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
                .name("volume")
                .uuid("b4368d05a2394f1fb75173683f55456f")
                .permissionsByName("volume")
                .excludeActions(APICreateVolumeSnapshotMsg.class)
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
