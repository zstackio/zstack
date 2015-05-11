package org.zstack.storage.backup.sftp;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;

import java.util.List;

public class APIQuerySftpBackupStorageReply extends APIQueryReply {
    private List<SftpBackupStorageInventory> inventories;

    public List<SftpBackupStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SftpBackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
}
