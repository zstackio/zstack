package org.zstack.ldap;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
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
 
    public static APICreateLdapBindingEvent __example__() {
        APICreateLdapBindingEvent event = new APICreateLdapBindingEvent();
        LdapAccountRefInventory inventory = new LdapAccountRefInventory();
        inventory.setUuid(uuid());
        inventory.setLdapUid("ou=Employee,uid=test");
        inventory.setAccountUuid(uuid());
        inventory.setLdapServerUuid(uuid());

        event.setInventory(inventory);
        return event;
    }

}
