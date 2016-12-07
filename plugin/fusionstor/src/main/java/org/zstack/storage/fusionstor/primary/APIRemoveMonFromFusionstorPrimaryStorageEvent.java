package org.zstack.storage.fusionstor.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/6/2015.
 */
@RestResponse(allTo = "inventory")
public class APIRemoveMonFromFusionstorPrimaryStorageEvent extends APIEvent {
    private FusionstorPrimaryStorageInventory inventory;

    public APIRemoveMonFromFusionstorPrimaryStorageEvent() {
    }

    public APIRemoveMonFromFusionstorPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public FusionstorPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(FusionstorPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
}
