package org.zstack.header.image;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryImageReply.class, inventoryClass = ImageInventory.class)
@Action(category = ImageConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryImageMsg extends APIQueryMessage {

}
