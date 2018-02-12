package org.zstack.sdk;

public class CreateVolumeSnapshotResult {
    public VolumeSnapshotInventory inventory;
    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeSnapshotInventory getInventory() {
        return this.inventory;
    }

}
