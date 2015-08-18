package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 8/18/2015.
 */
@AutoQuery(replyClass = APIQueryLoadBalancerReply.class, inventoryClass = LoadBalancerInventory.class)
public class APIQueryLoadBalancerMsg extends APIQueryMessage {
}
