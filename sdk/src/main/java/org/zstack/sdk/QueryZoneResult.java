package org.zstack.sdk;

public class QueryZoneResult {
    public java.util.List<ZoneInventory> inventories;
    public void setInventories(java.util.List<ZoneInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<ZoneInventory> getInventories() {
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
