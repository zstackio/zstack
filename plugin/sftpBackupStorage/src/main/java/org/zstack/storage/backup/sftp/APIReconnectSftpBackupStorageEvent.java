package org.zstack.storage.backup.sftp;

import org.zstack.header.message.APIEvent;

public class APIReconnectSftpBackupStorageEvent extends APIEvent {
    private SftpBackupStorageInventory inventory;
    
    public APIReconnectSftpBackupStorageEvent(String apiId) {
        super(apiId);
    }
    
    public APIReconnectSftpBackupStorageEvent() {
        super(null);
    }

    public SftpBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SftpBackupStorageInventory inventory) {
        this.inventory = inventory;
    }
}
