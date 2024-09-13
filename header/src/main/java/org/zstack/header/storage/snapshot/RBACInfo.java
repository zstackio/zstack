package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.volume.VolumeVO;

public class RBACInfo implements RBACDescription {
    @Override
    public String permissionName() {
        return "snapshot";
    }

    @Override
    public void permissions() {
        permissionBuilder()
                .targetResources(VolumeSnapshotVO.class, VolumeSnapshotTreeVO.class, VolumeVO.class)
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();
    }

    @Override
    public void roles() {
        roleBuilder()
                .uuid("a91363c6b4ba4e58966d17a4257668cd")
                .permissionBaseOnThis()
                .build();
    }
}
