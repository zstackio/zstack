package org.zstack.header.configuration;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryDiskOfferingReply.class, inventoryClass = DiskOfferingInventory.class)
public class APIQueryDiskOfferingMsg extends APIQueryMessage {

}
