package org.zstack.header.tag;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryUserTagReply.class, inventoryClass = UserTagInventory.class)
public class APIQueryUserTagMsg extends APIQueryMessage {
}
