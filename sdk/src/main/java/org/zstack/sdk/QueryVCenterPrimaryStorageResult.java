package org.zstack.sdk;

public class QueryVCenterPrimaryStorageResult {
    public java.util.List<VCenterPrimaryStorageInventory> inventories;
    public void setInventories(java.util.List<VCenterPrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VCenterPrimaryStorageInventory> getInventories() {
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
