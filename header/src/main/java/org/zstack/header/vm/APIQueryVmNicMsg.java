package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryVmNicReply.class, inventoryClass = VmNicInventory.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryVmNicMsg extends APIQueryMessage {

}
