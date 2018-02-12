package org.zstack.sdk;

public class QueryBaremetalHostCfgResult {
    public java.util.List<BaremetalHostCfgInventory> inventories;
    public void setInventories(java.util.List<BaremetalHostCfgInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<BaremetalHostCfgInventory> getInventories() {
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
