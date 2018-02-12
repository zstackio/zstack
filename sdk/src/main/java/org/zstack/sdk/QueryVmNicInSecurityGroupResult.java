package org.zstack.sdk;

public class QueryVmNicInSecurityGroupResult {
    public java.util.List<VmNicSecurityGroupRefInventory> inventories;
    public void setInventories(java.util.List<VmNicSecurityGroupRefInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<VmNicSecurityGroupRefInventory> getInventories() {
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
