package org.zstack.sdk;

public class QueryVolumeResult {
    public java.util.List<VolumeInventory> inventories;
    public void setInventories(java.util.List<VolumeInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VolumeInventory> getInventories() {
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
