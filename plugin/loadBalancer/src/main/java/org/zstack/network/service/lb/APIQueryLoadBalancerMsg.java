package org.zstack.network.service.lb;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 8/18/2015.
 */
@AutoQuery(replyClass = APIQueryLoadBalancerReply.class, inventoryClass = LoadBalancerInventory.class)
@Action(category = LoadBalancerConstants.ACTION_CATEGORY, names = {"read"})
public class APIQueryLoadBalancerMsg extends APIQueryMessage {
}
