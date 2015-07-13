package org.zstack.network.service.virtualrouter;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryVirtualRouterOfferingReply.class, inventoryClass = VirtualRouterOfferingInventory.class)
@Action(category = VirtualRouterConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryVirtualRouterOfferingMsg extends APIQueryMessage {

}
