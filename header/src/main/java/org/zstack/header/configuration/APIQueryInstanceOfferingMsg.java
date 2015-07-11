package org.zstack.header.configuration;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryInstanceOfferingReply.class, inventoryClass = InstanceOfferingInventory.class)
@Action(category = ConfigurationConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryInstanceOfferingMsg extends APIQueryMessage {

}
