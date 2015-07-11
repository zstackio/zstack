package org.zstack.header.tag;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQueryUserTagReply.class, inventoryClass = UserTagInventory.class)
@Action(category = TagConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryUserTagMsg extends APIQueryMessage {
}
