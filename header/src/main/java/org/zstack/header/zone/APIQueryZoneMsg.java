package org.zstack.header.zone;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryZoneReply.class, inventoryClass = ZoneInventory.class)
public class APIQueryZoneMsg extends APIQueryMessage {

}
