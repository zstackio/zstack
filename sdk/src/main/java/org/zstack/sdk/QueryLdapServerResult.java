package org.zstack.sdk;

public class QueryLdapServerResult {
    public java.util.List<LdapServerInventory> inventories;
    public void setInventories(java.util.List<LdapServerInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<LdapServerInventory> getInventories() {
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
