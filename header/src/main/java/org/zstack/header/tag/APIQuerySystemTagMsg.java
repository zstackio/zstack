package org.zstack.header.tag;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 */
@AutoQuery(replyClass = APIQuerySystemTagReply.class, inventoryClass = SystemTagInventory.class)
@Action(category = TagConstant.ACTION_CATEGORY, names = {"read"})
public class APIQuerySystemTagMsg extends APIQueryMessage {
}
