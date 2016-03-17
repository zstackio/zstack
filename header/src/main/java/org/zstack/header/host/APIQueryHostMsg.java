package org.zstack.header.host;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@Action(category = HostConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryHostReply.class, inventoryClass = HostInventory.class)
public class APIQueryHostMsg extends APIQueryMessage {

}
