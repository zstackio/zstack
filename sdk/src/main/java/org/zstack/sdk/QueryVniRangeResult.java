package org.zstack.sdk;

public class QueryVniRangeResult {
    public java.util.List<VniRangeInventory> inventories;
    public void setInventories(java.util.List<VniRangeInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VniRangeInventory> getInventories() {
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
