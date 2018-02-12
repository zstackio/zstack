package org.zstack.sdk;

public class QueryLdapBindingResult {
    public java.util.List<LdapAccountRefInventory> inventories;
    public void setInventories(java.util.List<LdapAccountRefInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<LdapAccountRefInventory> getInventories() {
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
