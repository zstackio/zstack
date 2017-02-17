package org.zstack.sdk;

public class QuerySecurityGroupRuleResult {
    public java.util.List<SecurityGroupRuleInventory> inventories;
    public void setInventories(java.util.List<SecurityGroupRuleInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<SecurityGroupRuleInventory> getInventories() {
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
