package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryVolumeReply.class, inventoryClass = VolumeInventory.class)
@Action(category = VolumeConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryVolumeMsg extends APIQueryMessage {

}
