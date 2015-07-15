package org.zstack.header.identity;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 7/14/2015.
 */
@AutoQuery(replyClass = APIQueryQuotaReply.class, inventoryClass = QuotaInventory.class)
@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryQuotaMsg extends APIQueryMessage {
}
