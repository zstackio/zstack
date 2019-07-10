package org.zstack.storage.surfs.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by zhouhaiping 2017-09-14
 */
@RestResponse(allTo = "inventory")
public class APIRemoveNodeFromSurfsPrimaryStorageEvent extends APIEvent {
    private SurfsPrimaryStorageInventory inventory;

    public APIRemoveNodeFromSurfsPrimaryStorageEvent() {
    }

    public APIRemoveNodeFromSurfsPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public SurfsPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SurfsPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRemoveNodeFromSurfsPrimaryStorageEvent __example__() {
        APIRemoveNodeFromSurfsPrimaryStorageEvent event = new APIRemoveNodeFromSurfsPrimaryStorageEvent();


        return event;
    }

}
