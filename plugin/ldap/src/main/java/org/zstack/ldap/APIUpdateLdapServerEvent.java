package org.zstack.ldap;

import org.zstack.header.message.APIEvent;

public class APIUpdateLdapServerEvent extends APIEvent {
    private LdapServerInventory inventory;

    public APIUpdateLdapServerEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateLdapServerEvent() {
        super(null);
    }

    public LdapServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }
}
