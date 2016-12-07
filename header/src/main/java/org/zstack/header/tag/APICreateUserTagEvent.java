package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateUserTagEvent extends APIEvent {
    private UserTagInventory inventory;

    public APICreateUserTagEvent(String apiId) {
        super(apiId);
    }

    public APICreateUserTagEvent() {
        super(null);
    }

    public UserTagInventory getInventory() {
        return inventory;
    }

    public void setInventory(UserTagInventory inventory) {
        this.inventory = inventory;
    }
}
