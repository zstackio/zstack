package org.zstack.sdk;

public class QueryNasFileSystemResult {
    public java.util.List<NasFileSystemInventory> inventories;
    public void setInventories(java.util.List<NasFileSystemInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<NasFileSystemInventory> getInventories() {
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
