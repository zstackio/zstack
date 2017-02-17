package org.zstack.sdk;

public class QueryUserResult {
    public java.util.List<UserInventory> inventories;
    public void setInventories(java.util.List<UserInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<UserInventory> getInventories() {
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
