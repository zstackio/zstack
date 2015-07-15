package org.zstack.header.identity;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 7/14/2015.
 */

@AutoQuery(replyClass = APIQueryUserGroupReply.class, inventoryClass = UserGroupInventory.class)
@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryUserGroupMsg extends APIQueryMessage {
}
