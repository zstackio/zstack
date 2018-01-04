package org.zstack.storage.surfs.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by zhouhaiping 2017-09-14
 */
@RestResponse(allTo = "inventory")
public class APIUpdateNodeToSurfsPrimaryStorageEvent extends APIEvent {
    private SurfsPrimaryStorageInventory inventory;

    public APIUpdateNodeToSurfsPrimaryStorageEvent() {
    }

    public APIUpdateNodeToSurfsPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public SurfsPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SurfsPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateNodeToSurfsPrimaryStorageEvent __example__() {
        APIUpdateNodeToSurfsPrimaryStorageEvent event = new APIUpdateNodeToSurfsPrimaryStorageEvent();


        return event;
    }

}
