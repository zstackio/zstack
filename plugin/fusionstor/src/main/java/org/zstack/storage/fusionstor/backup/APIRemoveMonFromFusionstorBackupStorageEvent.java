package org.zstack.storage.fusionstor.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/1/2015.
 */
@RestResponse(allTo = "inventory")
public class APIRemoveMonFromFusionstorBackupStorageEvent extends APIEvent {
    private FusionstorBackupStorageInventory inventory;

    public APIRemoveMonFromFusionstorBackupStorageEvent() {
    }

    public APIRemoveMonFromFusionstorBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public FusionstorBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(FusionstorBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
