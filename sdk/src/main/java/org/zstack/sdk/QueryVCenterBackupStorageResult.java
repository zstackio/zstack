package org.zstack.sdk;

public class QueryVCenterBackupStorageResult {
    public java.util.List<VCenterBackupStorageInventory> inventories;
    public void setInventories(java.util.List<VCenterBackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VCenterBackupStorageInventory> getInventories() {
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
