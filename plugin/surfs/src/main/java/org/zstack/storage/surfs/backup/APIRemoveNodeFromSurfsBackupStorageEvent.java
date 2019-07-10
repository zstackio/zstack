package org.zstack.storage.surfs.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/1/2015.
 */
@RestResponse(allTo = "inventory")
public class APIRemoveNodeFromSurfsBackupStorageEvent extends APIEvent {
    private SurfsBackupStorageInventory inventory;

    public APIRemoveNodeFromSurfsBackupStorageEvent() {
    }

    public APIRemoveNodeFromSurfsBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public SurfsBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SurfsBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRemoveNodeFromSurfsBackupStorageEvent __example__() {
        APIRemoveNodeFromSurfsBackupStorageEvent event = new APIRemoveNodeFromSurfsBackupStorageEvent();


        return event;
    }

}
