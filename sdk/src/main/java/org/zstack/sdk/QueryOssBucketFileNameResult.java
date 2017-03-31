package org.zstack.sdk;

public class QueryOssBucketFileNameResult {
    public java.util.List<OssBucketInventory> inventories;
    public void setInventories(java.util.List<OssBucketInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<OssBucketInventory> getInventories() {
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
