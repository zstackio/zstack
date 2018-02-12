package org.zstack.sdk;

public class QuerySecurityGroupResult {
    public java.util.List<SecurityGroupInventory> inventories;
    public void setInventories(java.util.List<SecurityGroupInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<SecurityGroupInventory> getInventories() {
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
