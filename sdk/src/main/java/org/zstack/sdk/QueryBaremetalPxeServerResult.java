package org.zstack.sdk;

public class QueryBaremetalPxeServerResult {
    public java.util.List<BaremetalPxeServerInventory> inventories;
    public void setInventories(java.util.List<BaremetalPxeServerInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<BaremetalPxeServerInventory> getInventories() {
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
