package org.zstack.header.vm;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryVmInstanceReply.class, inventoryClass = VmInstanceInventory.class)
public class APIQueryVmInstanceMsg extends APIQueryMessage {

}
