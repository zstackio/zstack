package org.zstack.sdk;

public class QueryDahoCloudConnectionResult {
    public java.util.List<DahoCloudConnectionInventory> inventories;
    public void setInventories(java.util.List<DahoCloudConnectionInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<DahoCloudConnectionInventory> getInventories() {
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
