package org.zstack.sdk;

public class QueryBaremetalHardwareInfoResult {
    public java.util.List<BaremetalHardwareInfoInventory> inventories;
    public void setInventories(java.util.List<BaremetalHardwareInfoInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<BaremetalHardwareInfoInventory> getInventories() {
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
