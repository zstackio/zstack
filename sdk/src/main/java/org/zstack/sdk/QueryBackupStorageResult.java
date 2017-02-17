package org.zstack.sdk;

public class QueryBackupStorageResult {
    public java.util.List<BackupStorageInventory> inventories;
    public void setInventories(java.util.List<BackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<BackupStorageInventory> getInventories() {
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
