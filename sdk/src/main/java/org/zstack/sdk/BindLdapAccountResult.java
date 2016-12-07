package org.zstack.sdk;

public class BindLdapAccountResult {
    public LdapAccountRefInventory inventory;
    public void setInventory(LdapAccountRefInventory inventory) {
        this.inventory = inventory;
    }
    public LdapAccountRefInventory getInventory() {
        return this.inventory;
    }

}
