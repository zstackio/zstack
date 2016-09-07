package org.zstack.ldap;

import org.zstack.header.message.APIEvent;

public class APIUnbindLdapAccountEvent extends APIEvent {
    private LdapAccountRefInventory inventory;

    public APIUnbindLdapAccountEvent(String apiId) {
        super(apiId);
    }

    public APIUnbindLdapAccountEvent() {
        super(null);
    }

    public LdapAccountRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(LdapAccountRefInventory inventory) {
        this.inventory = inventory;
    }
}
