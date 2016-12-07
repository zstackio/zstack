package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 8/18/2015.
 */
@AutoQuery(replyClass = APIQueryLoadBalancerListenerReply.class, inventoryClass = LoadBalancerListenerInventory.class)
@Action(category = LoadBalancerConstants.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/load-balancers/listeners",
        optionalPaths = { "/load-balancers/listeners/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLoadBalancerListenerReply.class
)
public class APIQueryLoadBalancerListenerMsg extends APIQueryMessage {
}
