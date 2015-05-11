package org.zstack.network.securitygroup;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQuerySecurityGroupReply.class, inventoryClass = SecurityGroupInventory.class)
public class APIQuerySecurityGroupMsg extends APIQueryMessage {

}
