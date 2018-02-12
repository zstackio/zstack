package org.zstack.sdk;

public class QueryDiskOfferingResult {
    public java.util.List<DiskOfferingInventory> inventories;
    public void setInventories(java.util.List<DiskOfferingInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<DiskOfferingInventory> getInventories() {
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
