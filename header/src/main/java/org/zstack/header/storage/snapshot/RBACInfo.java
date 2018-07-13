package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.volume.VolumeVO;

public class RBACInfo implements RBACDescription {
    @Override
    public void permissions() {
        permissionBuilder()
                .name("snapshot")
                .normalAPIs("org.zstack.header.storage.snapshot.**")
                .targetResources(VolumeSnapshotVO.class, VolumeSnapshotTreeVO.class, VolumeVO.class)
                .build();
    }

    @Override
    public void contributeToRoles() {

    }

    @Override
    public void roles() {
        roleBuilder()
                .name("snapshot")
                .uuid("a91363c6b4ba4e58966d17a4257668cd")
                .permissionsByName("snapshot")
                .build();
    }

    @Override
    public void globalReadableResources() {

    }
}
