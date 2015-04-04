package org.zstack.network.service.portforwarding;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryPortForwardingRuleReply.class, inventoryClass = PortForwardingRuleInventory.class)
public class APIQueryPortForwardingRuleMsg extends APIQueryMessage {

}
