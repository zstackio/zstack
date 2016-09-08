package org.zstack.ldap;

import org.zstack.header.message.APIEvent;

public class APITestAddLdapServerConnectionEvent extends APIEvent {
    private LdapServerInventory inventory;

    public APITestAddLdapServerConnectionEvent(String apiId) {
        super(apiId);
    }

    public APITestAddLdapServerConnectionEvent() {
        super(null);
    }

    public LdapServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }

}
