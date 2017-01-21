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
 
    public static APIAddSftpBackupStorageEvent __example__() {
        APIAddSftpBackupStorageEvent event = new APIAddSftpBackupStorageEvent();
        SftpBackupStorageInventory ssInventory = new SftpBackupStorageInventory();
        ssInventory.setHostname("192.168.0.1");
        ssInventory.setSshPort(8080);
        ssInventory.setUsername("tester");
        return event;
    }

}
