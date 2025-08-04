package org.zstack.header.storage.snapshot;

import org.zstack.header.storage.primary.PrimaryStorageType;

public interface VolumeInnerSnapshotPathParser {
    String getVolumePathFromSnapshot(VolumeSnapshotInventory snapshot);
    PrimaryStorageType getPrimaryStorageType();
}
