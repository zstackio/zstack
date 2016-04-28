package org.zstack.storage.fusionstor.primary;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply;

/**
 * Created by frank on 8/6/2015.
 */
@AutoQuery(replyClass = APIQueryPrimaryStorageReply.class, inventoryClass = FusionstorPrimaryStorageInventory.class)
public class APIQueryFusionstorPrimaryStorageMsg extends APIQueryMessage {
}
