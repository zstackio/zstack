package org.zstack.sdk;

public class QueryVCenterClusterResult {
    public java.util.List<VCenterClusterInventory> inventories;
    public void setInventories(java.util.List<VCenterClusterInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VCenterClusterInventory> getInventories() {
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
