package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdatePrimaryStorageEvent extends APIEvent {
    private PrimaryStorageInventory inventory;

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdatePrimaryStorageEvent() {
    }

    public APIUpdatePrimaryStorageEvent(String apiId) {
        super(apiId);
    }
}
