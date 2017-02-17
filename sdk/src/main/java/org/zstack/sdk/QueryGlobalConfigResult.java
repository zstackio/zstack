package org.zstack.sdk;

public class QueryGlobalConfigResult {
    public java.util.List<GlobalConfigInventory> inventories;
    public void setInventories(java.util.List<GlobalConfigInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<GlobalConfigInventory> getInventories() {
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
