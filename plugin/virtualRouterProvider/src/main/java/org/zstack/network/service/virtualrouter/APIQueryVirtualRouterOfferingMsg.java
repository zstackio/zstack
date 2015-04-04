package org.zstack.network.service.virtualrouter;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryVirtualRouterOfferingReply.class, inventoryClass = VirtualRouterOfferingInventory.class)
public class APIQueryVirtualRouterOfferingMsg extends APIQueryMessage {

}
