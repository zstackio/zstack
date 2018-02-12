package org.zstack.sdk;

public class AddLdapServerResult {
    public LdapServerInventory inventory;
    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }
    public LdapServerInventory getInventory() {
        return this.inventory;
    }

}
