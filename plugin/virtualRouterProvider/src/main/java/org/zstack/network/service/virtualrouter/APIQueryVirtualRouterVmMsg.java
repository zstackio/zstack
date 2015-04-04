package org.zstack.network.service.virtualrouter;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryVirtualRouterVmReply.class, inventoryClass = VirtualRouterVmInventory.class)
public class APIQueryVirtualRouterVmMsg extends APIQueryMessage {
}
