package org.zstack.header.identity;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 7/14/2015.
 */
@AutoQuery(replyClass = APIQueryAccountReply.class, inventoryClass = AccountInventory.class)
public class APIQueryAccountMsg extends APIQueryMessage {
}
