package org.zstack.sdk;

public class QueryBaremetalChassisResult {
    public java.util.List<BaremetalChassisInventory> inventories;
    public void setInventories(java.util.List<BaremetalChassisInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<BaremetalChassisInventory> getInventories() {
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
