package org.zstack.storage.surfs.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/6/2015.
 */
@RestResponse(allTo = "inventory")
public class APIAddNodeToSurfsPrimaryStorageEvent extends APIEvent {
    private SurfsPrimaryStorageInventory inventory;

    public APIAddNodeToSurfsPrimaryStorageEvent() {
    }

    public APIAddNodeToSurfsPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public SurfsPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SurfsPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddNodeToSurfsPrimaryStorageEvent __example__() {
        APIAddNodeToSurfsPrimaryStorageEvent event = new APIAddNodeToSurfsPrimaryStorageEvent();


        return event;
    }

}
