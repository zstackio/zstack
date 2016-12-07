package org.zstack.storage.backup.sftp;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddSftpBackupStorageEvent extends APIEvent {
    public APIAddSftpBackupStorageEvent(String apiId) {
        super(apiId);
    }
    
    public APIAddSftpBackupStorageEvent() {
        super(null);
    }

    private SftpBackupStorageInventory inventory;

    public SftpBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SftpBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
