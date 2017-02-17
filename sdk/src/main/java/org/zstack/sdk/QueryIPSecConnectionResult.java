package org.zstack.sdk;

public class QueryIPSecConnectionResult {
    public java.util.List<IPsecConnectionInventory> inventories;
    public void setInventories(java.util.List<IPsecConnectionInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<IPsecConnectionInventory> getInventories() {
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
