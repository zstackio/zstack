package org.zstack.sdk;

public class QueryCephPrimaryStoragePoolResult {
    public java.util.List<CephPrimaryStoragePoolInventory> inventories;
    public void setInventories(java.util.List<CephPrimaryStoragePoolInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<CephPrimaryStoragePoolInventory> getInventories() {
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
