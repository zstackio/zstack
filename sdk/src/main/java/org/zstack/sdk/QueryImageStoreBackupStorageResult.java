package org.zstack.sdk;

public class QueryImageStoreBackupStorageResult {
    public java.util.List<ImageStoreBackupStorageInventory> inventories;
    public void setInventories(java.util.List<ImageStoreBackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<ImageStoreBackupStorageInventory> getInventories() {
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
