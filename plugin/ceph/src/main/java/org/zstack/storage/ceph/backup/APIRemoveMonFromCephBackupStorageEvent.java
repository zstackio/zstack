package org.zstack.storage.ceph.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/1/2015.
 */
@RestResponse(allTo = "inventory")
public class APIRemoveMonFromCephBackupStorageEvent extends APIEvent {
    private CephBackupStorageInventory inventory;

    public APIRemoveMonFromCephBackupStorageEvent() {
    }

    public APIRemoveMonFromCephBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public CephBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIRemoveMonFromCephBackupStorageEvent __example__() {
        APIRemoveMonFromCephBackupStorageEvent event = new APIRemoveMonFromCephBackupStorageEvent();


        return event;
    }

}
