package org.zstack.sdk;

public class SnapshotLeafInventory  {

    public VolumeSnapshotInventory inventory;
    public void setInventory(VolumeSnapshotInventory inventory) {
        this.inventory = inventory;
    }
    public VolumeSnapshotInventory getInventory() {
        return this.inventory;
    }

    public java.lang.String parentUuid;
    public void setParentUuid(java.lang.String parentUuid) {
        this.parentUuid = parentUuid;
    }
    public java.lang.String getParentUuid() {
        return this.parentUuid;
    }

    public java.util.List<SnapshotLeafInventory> children;
    public void setChildren(java.util.List<SnapshotLeafInventory> children) {
        this.children = children;
    }
    public java.util.List<SnapshotLeafInventory> getChildren() {
        return this.children;
    }

}
