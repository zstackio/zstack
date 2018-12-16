package org.zstack.header.storage.snapshot;

public interface VolumeSnapshotDeletionProtector {
    String getPrimaryStorageType();

    void protect(VolumeSnapshotInventory snapshot);
}
