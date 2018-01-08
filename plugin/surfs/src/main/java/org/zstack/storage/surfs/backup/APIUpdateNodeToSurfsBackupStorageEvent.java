package org.zstack.storage.surfs.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by zhouhaiping 2017-09-13
 */
@RestResponse(allTo = "inventory")
public class APIUpdateNodeToSurfsBackupStorageEvent extends APIEvent {
    private SurfsBackupStorageInventory inventory;

    public APIUpdateNodeToSurfsBackupStorageEvent() {
    }

    public APIUpdateNodeToSurfsBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public SurfsBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SurfsBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateNodeToSurfsBackupStorageEvent __example__() {
        APIUpdateNodeToSurfsBackupStorageEvent event = new APIUpdateNodeToSurfsBackupStorageEvent();


        return event;
    }

}
