package org.zstack.storage.primary.local;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 11/16/2015.
 */
@RestResponse(allTo = "inventory")
public class APILocalStorageMigrateVolumeEvent extends APIEvent {
    private LocalStorageResourceRefInventory inventory;

    public APILocalStorageMigrateVolumeEvent() {
    }

    public APILocalStorageMigrateVolumeEvent(String apiId) {
        super(apiId);
    }

    public LocalStorageResourceRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(LocalStorageResourceRefInventory inventory) {
        this.inventory = inventory;
    }
}
