package org.zstack.storage.primary.local;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 11/14/2015.
 */
@AutoQuery(replyClass = APIQueryLocalStorageResourceRefReply.class, inventoryClass = LocalStorageResourceRefInventory.class)
public class APIQueryLocalStorageResourceRefMsg extends APIQueryMessage {
}
