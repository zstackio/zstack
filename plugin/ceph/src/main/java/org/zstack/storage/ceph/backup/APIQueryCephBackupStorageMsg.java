package org.zstack.storage.ceph.backup;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.storage.backup.APIQueryBackupStorageReply;

/**
 * Created by frank on 8/6/2015.
 */
@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = CephBackupStorageInventory.class)
public class APIQueryCephBackupStorageMsg extends APIQueryMessage {
}
