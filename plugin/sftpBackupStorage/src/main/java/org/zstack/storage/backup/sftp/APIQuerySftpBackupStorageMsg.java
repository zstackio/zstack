package org.zstack.storage.backup.sftp;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.storage.backup.BackupStorageConstant;

@Action(category = BackupStorageConstant.ACTION_CATEGORY, names={"read"})
@AutoQuery(replyClass = APIQuerySftpBackupStorageReply.class, inventoryClass = SftpBackupStorageInventory.class)
public class APIQuerySftpBackupStorageMsg extends APIQueryMessage {

}
