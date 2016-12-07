package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryPortForwardingRuleReply.class, inventoryClass = PortForwardingRuleInventory.class)
@Action(category = PortForwardingConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/port-forwarding",
        method = HttpMethod.GET,
        optionalPaths = {"/port-forwarding/{uuid}"},
        responseClass = APIQueryPortForwardingRuleReply.class
)
public class APIQueryPortForwardingRuleMsg extends APIQueryMessage {

}
