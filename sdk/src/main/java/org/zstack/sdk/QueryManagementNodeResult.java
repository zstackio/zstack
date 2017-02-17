package org.zstack.sdk;

public class QueryManagementNodeResult {
    public java.util.List<ManagementNodeInventory> inventories;
    public void setInventories(java.util.List<ManagementNodeInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<ManagementNodeInventory> getInventories() {
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
