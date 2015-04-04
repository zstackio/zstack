package org.zstack.appliancevm;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryApplianceVmReply.class, inventoryClass = ApplianceVmInventory.class)
public class APIQueryApplianceVmMsg extends APIQueryMessage {
}
