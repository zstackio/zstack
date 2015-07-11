package org.zstack.network.service.vip;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
@AutoQuery(replyClass = APIQueryVipReply.class, inventoryClass = VipInventory.class)
@Action(category = VipConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryVipMsg extends APIQueryMessage {
}
