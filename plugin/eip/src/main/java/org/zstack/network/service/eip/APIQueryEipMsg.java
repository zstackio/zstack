package org.zstack.network.service.eip;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryEipReply.class, inventoryClass = EipInventory.class)
@Action(category = EipConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryEipMsg extends APIQueryMessage {
}
