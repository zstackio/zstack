package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryVmInstanceReply.class, inventoryClass = VmInstanceInventory.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryVmInstanceMsg extends APIQueryMessage {

}
