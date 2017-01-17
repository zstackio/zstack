package org.zstack.storage.fusionstor.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/6/2015.
 */
@RestResponse(allTo = "inventory")
public class APIAddMonToFusionstorPrimaryStorageEvent extends APIEvent {
    private FusionstorPrimaryStorageInventory inventory;

    public APIAddMonToFusionstorPrimaryStorageEvent() {
    }

    public APIAddMonToFusionstorPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public FusionstorPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(FusionstorPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddMonToFusionstorPrimaryStorageEvent __example__() {
        APIAddMonToFusionstorPrimaryStorageEvent event = new APIAddMonToFusionstorPrimaryStorageEvent();


        return event;
    }

}
