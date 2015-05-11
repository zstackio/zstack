package org.zstack.header.host;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryHostReply.class, inventoryClass = HostInventory.class)
public class APIQueryHostMsg extends APIQueryMessage {

}
