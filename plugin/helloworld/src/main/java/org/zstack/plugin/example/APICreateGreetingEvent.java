package org.zstack.plugin.example;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateGreetingEvent extends APIEvent {
    private GreetingInventory inventory;

    public APICreateGreetingEvent() {
    }

    public APICreateGreetingEvent(String apiId) {
        super(apiId);
    }

    public GreetingInventory getInventory() {
        return inventory;
    }

    public void setInventory(GreetingInventory inventory) {
        this.inventory = inventory;
    }
}
