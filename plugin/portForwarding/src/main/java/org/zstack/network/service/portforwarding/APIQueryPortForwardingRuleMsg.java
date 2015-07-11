package org.zstack.network.service.portforwarding;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryPortForwardingRuleReply.class, inventoryClass = PortForwardingRuleInventory.class)
@Action(category = PortForwardingConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryPortForwardingRuleMsg extends APIQueryMessage {

}
