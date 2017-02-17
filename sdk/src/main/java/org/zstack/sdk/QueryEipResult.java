package org.zstack.sdk;

public class QueryEipResult {
    public java.util.List<EipInventory> inventories;
    public void setInventories(java.util.List<EipInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<EipInventory> getInventories() {
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
