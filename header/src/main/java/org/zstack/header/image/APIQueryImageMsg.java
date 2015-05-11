package org.zstack.header.image;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryImageReply.class, inventoryClass = ImageInventory.class)
public class APIQueryImageMsg extends APIQueryMessage {

}
