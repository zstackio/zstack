package org.zstack.header.storage.snapshot;

import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;

/**
 */
public class VolumeSnapshotStruct {
    private VolumeSnapshotInventory parent;
    private VolumeSnapshotInventory current;
    private boolean fullSnapshot;
    private VolumeSnapshotArrangementType arrangementType;

    public boolean isFullSnapshot() {
        return fullSnapshot;
    }

    public void setFullSnapshot(boolean fullSnapshot) {
        this.fullSnapshot = fullSnapshot;
    }

    public VolumeSnapshotInventory getParent() {
        return parent;
    }

    public void setParent(VolumeSnapshotInventory parent) {
        this.parent = parent;
    }

    public VolumeSnapshotInventory getCurrent() {
        return current;
    }

    public void setCurrent(VolumeSnapshotInventory current) {
        this.current = current;
    }

    public VolumeSnapshotArrangementType getArrangementType() {
        return arrangementType;
    }

    public void setArrangementType(VolumeSnapshotArrangementType arrangementType) {
        this.arrangementType = arrangementType;
    }
}
