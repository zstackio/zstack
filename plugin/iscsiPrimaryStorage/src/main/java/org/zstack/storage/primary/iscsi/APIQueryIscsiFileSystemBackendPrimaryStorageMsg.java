package org.zstack.storage.primary.iscsi;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 4/27/2015.
 */
@AutoQuery(replyClass = APIQueryIscsiFileSystemBackendPrimaryStorageReply.class, inventoryClass = IscsiFileSystemBackendPrimaryStorageInventory.class)
public class APIQueryIscsiFileSystemBackendPrimaryStorageMsg extends APIQueryMessage {
}
