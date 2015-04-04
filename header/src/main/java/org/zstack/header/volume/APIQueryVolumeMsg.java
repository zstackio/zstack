package org.zstack.header.volume;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryVolumeReply.class, inventoryClass = VolumeInventory.class)
public class APIQueryVolumeMsg extends APIQueryMessage {

}
