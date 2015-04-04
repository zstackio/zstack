package org.zstack.network.service.eip;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryEipReply.class, inventoryClass = EipInventory.class)
public class APIQueryEipMsg extends APIQueryMessage {
}
