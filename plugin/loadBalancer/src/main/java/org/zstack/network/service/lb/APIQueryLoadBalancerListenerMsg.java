package org.zstack.network.service.lb;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

/**
 * Created by frank on 8/18/2015.
 */
@AutoQuery(replyClass = APIQueryLoadBalancerListenerReply.class, inventoryClass = LoadBalancerListenerInventory.class)
public class APIQueryLoadBalancerListenerMsg extends APIQueryMessage {
}
