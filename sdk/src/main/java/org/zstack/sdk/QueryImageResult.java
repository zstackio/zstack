package org.zstack.sdk;

public class QueryImageResult {
    public java.util.List<ImageInventory> inventories;
    public void setInventories(java.util.List<ImageInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<ImageInventory> getInventories() {
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
