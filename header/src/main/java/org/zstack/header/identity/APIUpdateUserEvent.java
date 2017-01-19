package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/10/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateUserEvent extends APIEvent {
    private UserInventory inventory;

    public UserInventory getInventory() {
        return inventory;
    }

    public void setInventory(UserInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateUserEvent() {
    }

    public APIUpdateUserEvent(String apiId) {
        super(apiId);
    }
 
    public static APIUpdateUserEvent __example__() {
        APIUpdateUserEvent event = new APIUpdateUserEvent();
        UserInventory inventory = new UserInventory();
        inventory.setName("new");
        inventory.setAccountUuid(uuid());
        inventory.setUuid(uuid());
        return event;
    }

}
