package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateAccessKeyEvent extends APIEvent {
    private AccessKeyInventory inventory;

    public APICreateAccessKeyEvent(String apiId) {
        super(apiId);
    }

    public APICreateAccessKeyEvent() {
        super(null);
    }

    public AccessKeyInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccessKeyInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateAccessKeyEvent __example__() {
        APICreateAccessKeyEvent event = new APICreateAccessKeyEvent();

        AccessKeyInventory inventory = new AccessKeyInventory();
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        inventory.setUserUuid(uuid());
        inventory.setAccessKeyID("1234567890abcdedfhij");
        inventory.setAccessKeySecret("1234567890abcdedfhij1234567890abcdedfhij");

        event.setInventory(inventory);
        return event;
    }

}
