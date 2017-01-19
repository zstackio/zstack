package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateUserGroupEvent extends APIEvent {
    private UserGroupInventory inventory;

    public APICreateUserGroupEvent(String apiId) {
        super(apiId);
    }

    public APICreateUserGroupEvent() {
        super(null);
    }

    public UserGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(UserGroupInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateUserGroupEvent __example__() {
        APICreateUserGroupEvent event = new APICreateUserGroupEvent();

        UserGroupInventory inventory = new UserGroupInventory();
        inventory.setName("usergroup");
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());

        event.setInventory(inventory);
        return event;
    }

}
