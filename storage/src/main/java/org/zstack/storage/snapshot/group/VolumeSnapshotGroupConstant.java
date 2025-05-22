package org.zstack.storage.snapshot.group;

public interface VolumeSnapshotGroupConstant {
    enum Parmas {
        SnapshotGroupUuid,
        SnapshotGroup
    }

    String SKIP_RESOURCE_ROLLBACK = "skipResourceRollback";
}
