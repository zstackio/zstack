package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 */
public class APICreateTagEvent extends APIEvent {
    private TagInventory inventory;

    public TagInventory getInventory() {
        return inventory;
    }

    public void setInventory(TagInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateTagEvent(String apiId) {
        super(apiId);
    }

    public APICreateTagEvent() {
        super(null);
    }
}
