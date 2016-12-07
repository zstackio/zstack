package org.zstack.ldap;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddLdapServerEvent extends APIEvent {
    private LdapServerInventory inventory;

    public APIAddLdapServerEvent(String apiId) {
        super(apiId);
    }

    public APIAddLdapServerEvent() {
        super(null);
    }

    public LdapServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }
}
