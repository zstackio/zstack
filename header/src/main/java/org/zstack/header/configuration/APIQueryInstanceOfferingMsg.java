package org.zstack.header.configuration;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryInstanceOfferingReply.class, inventoryClass = InstanceOfferingInventory.class)
public class APIQueryInstanceOfferingMsg extends APIQueryMessage {

}
