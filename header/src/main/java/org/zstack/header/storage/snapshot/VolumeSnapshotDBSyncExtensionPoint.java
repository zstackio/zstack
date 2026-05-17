package org.zstack.header.storage.snapshot;

import org.zstack.header.volume.VolumeInventory;

public interface VolumeSnapshotDBSyncExtensionPoint {
    VolumeSnapshotInventory syncVolumeSnapshotDBAfterTakeSnapshot(VolumeInventory volume,
                                                                  VolumeSnapshotInventory snapshot,
                                                                  String volumeNewInstallPath);
}
