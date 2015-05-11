package org.zstack.header.tag;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQuerySystemTagReply.class, inventoryClass = SystemTagInventory.class)
public class APIQuerySystemTagMsg extends APIQueryMessage {
}
