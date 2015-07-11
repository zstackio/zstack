package org.zstack.network.securitygroup;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQuerySecurityGroupRuleReply.class, inventoryClass = SecurityGroupRuleInventory.class)
@Action(category = SecurityGroupConstant.ACTION_CATEGORY, names = {"read"})
public class APIQuerySecurityGroupRuleMsg extends APIQueryMessage {
}
