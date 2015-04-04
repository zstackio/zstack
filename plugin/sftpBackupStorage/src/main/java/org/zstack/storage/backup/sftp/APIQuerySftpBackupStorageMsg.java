package org.zstack.storage.backup.sftp;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQuerySftpBackupStorageReply.class, inventoryClass = SftpBackupStorageInventory.class)
public class APIQuerySftpBackupStorageMsg extends APIQueryMessage {

}
