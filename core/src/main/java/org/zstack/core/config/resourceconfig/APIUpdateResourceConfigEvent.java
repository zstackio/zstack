package org.zstack.core.config.resourceconfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateResourceConfigEvent extends APIEvent {
    private ResourceConfigInventory inventory;

    public APIUpdateResourceConfigEvent() {
    }

    public APIUpdateResourceConfigEvent(String apiId) {
        super(apiId);
    }

    public ResourceConfigInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceConfigInventory inventory) {
        this.inventory = inventory;
    }
}
