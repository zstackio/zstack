package org.zstack.sdk;

public class QueryResourcePriceResult {
    public java.util.List<PriceInventory> inventories;
    public void setInventories(java.util.List<PriceInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<PriceInventory> getInventories() {
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
