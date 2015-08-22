package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.APIQueryApplianceVmMsg;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryVirtualRouterVmReply.class, inventoryClass = VirtualRouterVmInventory.class)
@Action(category = VirtualRouterConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryVirtualRouterVmMsg extends APIQueryApplianceVmMsg {
}
