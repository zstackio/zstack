package org.zstack.sdk;

public class QueryVmNicResult {
    public java.util.List<VmNicInventory> inventories;
    public void setInventories(java.util.List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VmNicInventory> getInventories() {
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
