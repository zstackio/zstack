package org.zstack.storage.surfs.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by zhouhaiping 2017-09-12
 */
@RestResponse(allTo = "inventory")
public class APIAddNodeToSurfsBackupStorageEvent extends APIEvent {
    private SurfsBackupStorageInventory inventory;

    public APIAddNodeToSurfsBackupStorageEvent() {
    }

    public APIAddNodeToSurfsBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public SurfsBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SurfsBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddNodeToSurfsBackupStorageEvent __example__() {
        APIAddNodeToSurfsBackupStorageEvent event = new APIAddNodeToSurfsBackupStorageEvent();


        return event;
    }

}
