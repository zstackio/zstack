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
 
    public static APIAddLdapServerEvent __example__() {
        APIAddLdapServerEvent event = new APIAddLdapServerEvent();
        LdapServerInventory inventory = new LdapServerInventory();
        inventory.setUuid(uuid());
        inventory.setName("miao");
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
