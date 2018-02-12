package org.zstack.sdk;

public class QueryHostResult {
    public java.util.List<HostInventory> inventories;
    public void setInventories(java.util.List<HostInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<HostInventory> getInventories() {
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
