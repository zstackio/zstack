package org.zstack.sdk;

import org.zstack.sdk.LdapServerInventory;

public class UpdateLdapServerResult {
    public LdapServerInventory inventory;
    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }
    public LdapServerInventory getInventory() {
        return this.inventory;
    }

}
