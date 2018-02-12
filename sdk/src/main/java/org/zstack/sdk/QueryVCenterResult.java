package org.zstack.sdk;

public class QueryVCenterResult {
    public java.util.List<VCenterInventory> inventories;
    public void setInventories(java.util.List<VCenterInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VCenterInventory> getInventories() {
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
