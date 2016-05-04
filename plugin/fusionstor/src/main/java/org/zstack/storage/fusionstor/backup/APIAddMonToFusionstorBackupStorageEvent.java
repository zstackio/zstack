package org.zstack.storage.fusionstor.backup;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 8/1/2015.
 */
public class APIAddMonToFusionstorBackupStorageEvent extends APIEvent {
    private FusionstorBackupStorageInventory inventory;

    public APIAddMonToFusionstorBackupStorageEvent() {
    }

    public APIAddMonToFusionstorBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public FusionstorBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(FusionstorBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
