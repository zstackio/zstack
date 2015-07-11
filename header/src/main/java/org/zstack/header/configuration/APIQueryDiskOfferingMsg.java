package org.zstack.header.configuration;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryDiskOfferingReply.class, inventoryClass = DiskOfferingInventory.class)
@Action(category = ConfigurationConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryDiskOfferingMsg extends APIQueryMessage {

}
