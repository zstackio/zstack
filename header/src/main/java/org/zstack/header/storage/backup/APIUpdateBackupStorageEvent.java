package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateBackupStorageEvent extends APIEvent {
    private BackupStorageInventory inventory;

    public APIUpdateBackupStorageEvent() {
    }

    public APIUpdateBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public BackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(BackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
