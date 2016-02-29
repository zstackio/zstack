package org.zstack.header.identity;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 2/23/2016.
 */
@AutoQuery(replyClass = APIQuerySharedResourceReply.class, inventoryClass = SharedResourceInventory.class)
public class APIQuerySharedResourceMsg extends APIQueryMessage {
}
