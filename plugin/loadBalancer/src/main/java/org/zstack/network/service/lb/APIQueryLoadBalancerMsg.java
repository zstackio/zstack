package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 8/18/2015.
 */
@AutoQuery(replyClass = APIQueryLoadBalancerReply.class, inventoryClass = LoadBalancerInventory.class)
@Action(category = LoadBalancerConstants.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/load-balancers",
        optionalPaths = {"/load-balancers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLoadBalancerReply.class
)
public class APIQueryLoadBalancerMsg extends APIQueryMessage {
}
