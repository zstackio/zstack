package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 4/23/2015.
 */
@RestResponse(allTo = "inventory")
public class APIReconnectPrimaryStorageEvent extends APIEvent {
    private PrimaryStorageInventory inventory;

    public APIReconnectPrimaryStorageEvent() {
    }

    public APIReconnectPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
}
