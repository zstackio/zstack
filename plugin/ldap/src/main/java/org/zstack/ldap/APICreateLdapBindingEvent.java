package org.zstack.ldap;

import org.zstack.header.message.APIEvent;

public class APICreateLdapBindingEvent extends APIEvent {
    private LdapAccountRefInventory inventory;

    public APICreateLdapBindingEvent(String apiId) {
        super(apiId);
    }

    public APICreateLdapBindingEvent() {
        super(null);
    }

    public LdapAccountRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(LdapAccountRefInventory inventory) {
        this.inventory = inventory;
    }
}
