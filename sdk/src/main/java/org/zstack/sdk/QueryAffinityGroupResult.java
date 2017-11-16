package org.zstack.sdk;

public class QueryAffinityGroupResult {
    public java.util.List<AffinityGroupInventory> inventories;
    public void setInventories(java.util.List<AffinityGroupInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<AffinityGroupInventory> getInventories() {
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
