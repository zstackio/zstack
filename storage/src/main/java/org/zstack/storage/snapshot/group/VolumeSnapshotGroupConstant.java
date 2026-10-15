package org.zstack.storage.snapshot.group;

public interface VolumeSnapshotGroupConstant {
    String RESTORE_COMPLETED_PATH = "/snapshot/group/restore/%s/completed";

    enum Parmas {
        SnapshotGroupUuid,
        SnapshotGroup
    }
}
