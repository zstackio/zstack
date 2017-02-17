package org.zstack.sdk;

public class QueryUserGroupResult {
    public java.util.List<UserGroupInventory> inventories;
    public void setInventories(java.util.List<UserGroupInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<UserGroupInventory> getInventories() {
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
