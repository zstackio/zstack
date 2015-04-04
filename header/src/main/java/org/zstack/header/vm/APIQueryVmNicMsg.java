package org.zstack.header.vm;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryVmNicReply.class, inventoryClass = VmNicInventory.class)
public class APIQueryVmNicMsg extends APIQueryMessage {

}
