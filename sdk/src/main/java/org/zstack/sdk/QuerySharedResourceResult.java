package org.zstack.sdk;

public class QuerySharedResourceResult {
    public java.util.List<SharedResourceInventory> inventories;
    public void setInventories(java.util.List<SharedResourceInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<SharedResourceInventory> getInventories() {
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
