package org.zstack.header.storage.backup;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = BackupStorageInventory.class)
public class APIQueryBackupStorageMsg extends APIQueryMessage {

}
