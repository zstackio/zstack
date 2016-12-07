package org.zstack.storage.fusionstor.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Mei Lei on 6/6/2016.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateMonToFusionstorBackupStorageEvent extends APIEvent {
    private FusionstorBackupStorageInventory inventory;

    public APIUpdateMonToFusionstorBackupStorageEvent() {
    }

    public APIUpdateMonToFusionstorBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public FusionstorBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(FusionstorBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
