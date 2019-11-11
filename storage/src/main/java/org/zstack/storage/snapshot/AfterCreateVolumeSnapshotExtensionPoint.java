package org.zstack.storage.snapshot;

import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 * Created by GuoYi on 11/15/19.
 */
public interface AfterCreateVolumeSnapshotExtensionPoint {
    void afterCreateVolumeSnapshot(VolumeSnapshotInventory snapshot);
}
