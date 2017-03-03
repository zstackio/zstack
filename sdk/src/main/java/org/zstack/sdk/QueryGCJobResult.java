package org.zstack.sdk;

public class QueryGCJobResult {
    public java.util.List<GarbageCollectorInventory> inventories;
    public void setInventories(java.util.List<GarbageCollectorInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<GarbageCollectorInventory> getInventories() {
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
