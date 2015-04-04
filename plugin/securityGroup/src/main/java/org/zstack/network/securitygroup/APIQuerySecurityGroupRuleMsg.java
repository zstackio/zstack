package org.zstack.network.securitygroup;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQuerySecurityGroupRuleReply.class, inventoryClass = SecurityGroupRuleInventory.class)
public class APIQuerySecurityGroupRuleMsg extends APIQueryMessage {
}
