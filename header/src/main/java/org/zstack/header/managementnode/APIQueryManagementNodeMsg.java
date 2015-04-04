package org.zstack.header.managementnode;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryManagementNodeReply.class, inventoryClass = ManagementNodeInventory.class)
public class APIQueryManagementNodeMsg extends APIQueryMessage {
}
