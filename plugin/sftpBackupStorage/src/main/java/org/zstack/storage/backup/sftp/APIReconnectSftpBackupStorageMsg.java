package org.zstack.storage.backup.sftp;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.storage.backup.BackupStorageMessage;

public class APIReconnectSftpBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = SftpBackupStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }

    public void setUuid(String backupStorageUuid) {
        this.uuid = backupStorageUuid;
    }
}
