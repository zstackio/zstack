package org.zstack.ldap;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
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
 
    public static APIUpdateLdapServerEvent __example__() {
        APIUpdateLdapServerEvent event = new APIUpdateLdapServerEvent();
        LdapServerInventory inventory = new LdapServerInventory();
        inventory.setUuid(uuid());
        inventory.setName("new name");
        inventory.setDescription("miao desc");
        inventory.setUrl("ldap://localhost:1888");
        inventory.setBase("dc=example,dc=com");
        inventory.setUsername("");
        inventory.setPassword("");
        inventory.setEncryption("None");

        event.setInventory(inventory);
        return event;
    }

}
