package org.zstack.sdk;

import org.zstack.sdk.LdapAccountRefInventory;

public class CreateLdapBindingResult {
    public LdapAccountRefInventory inventory;
    public void setInventory(LdapAccountRefInventory inventory) {
        this.inventory = inventory;
    }
    public LdapAccountRefInventory getInventory() {
        return this.inventory;
    }

}
