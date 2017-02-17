package org.zstack.sdk;

public class QueryVolumeSnapshotResult {
    public java.util.List<VolumeSnapshotInventory> inventories;
    public void setInventories(java.util.List<VolumeSnapshotInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VolumeSnapshotInventory> getInventories() {
        return this.inventories;
    }

    public java.lang.Long total;
    public void setTotal(java.lang.Long total) {
        this.total = total;
    }
    public java.lang.Long getTotal() {
        return this.total;
    }

}
