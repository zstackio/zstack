package org.zstack.sdk;

public class QuerySftpBackupStorageResult {
    public java.util.List<SftpBackupStorageInventory> inventories;
    public void setInventories(java.util.List<SftpBackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<SftpBackupStorageInventory> getInventories() {
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
