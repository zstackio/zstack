package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.appliancevm.APIQueryApplianceVmMsg;
import org.zstack.appliancevm.APIQueryApplianceVmReply;
import org.zstack.header.identity.Action;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQueryVirtualRouterVmReply.class, inventoryClass = VirtualRouterVmInventory.class)
@Action(category = VirtualRouterConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/appliances/virtual-routers",
        optionalPaths = {"/vm-instances/appliances/virtual-routers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryApplianceVmReply.class
)
public class APIQueryVirtualRouterVmMsg extends APIQueryApplianceVmMsg {
}
