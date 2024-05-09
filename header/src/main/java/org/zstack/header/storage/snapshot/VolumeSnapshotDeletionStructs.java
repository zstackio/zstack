package org.zstack.header.storage.snapshot;

import java.util.List;

public class VolumeSnapshotDeletionStructs {
    List<VolumeSnapshotInventory> snapshotInventories;
    boolean onlySelf;

    public VolumeSnapshotDeletionStructs(List<VolumeSnapshotInventory> snapshotInventories, boolean onlySelf) {
        this.snapshotInventories = snapshotInventories;
        this.onlySelf = onlySelf;
    }

    public List<VolumeSnapshotInventory> getSnapshotInventories() {
        return snapshotInventories;
    }

    public void setSnapshotInventories(List<VolumeSnapshotInventory> snapshotInventories) {
        this.snapshotInventories = snapshotInventories;
    }

    public boolean isOnlySelf() {
        return onlySelf;
    }

    public void setOnlySelf(boolean onlySelf) {
        this.onlySelf = onlySelf;
    }
}
