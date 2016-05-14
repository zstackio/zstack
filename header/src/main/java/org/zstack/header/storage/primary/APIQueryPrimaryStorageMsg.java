package org.zstack.header.storage.primary;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryPrimaryStorageReply.class, inventoryClass = PrimaryStorageInventory.class)
public class APIQueryPrimaryStorageMsg extends APIQueryMessage {
}
