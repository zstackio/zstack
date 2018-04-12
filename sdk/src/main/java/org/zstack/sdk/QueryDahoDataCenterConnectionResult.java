package org.zstack.sdk;

public class QueryDahoDataCenterConnectionResult {
    public java.util.List<DahoConnectionInventory> inventories;
    public void setInventories(java.util.List<DahoConnectionInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<DahoConnectionInventory> getInventories() {
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
