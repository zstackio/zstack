package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryIpRangeReply.class, inventoryClass = IpRangeInventory.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/l3-networks/ip-ranges",
        optionalPaths = {"l3-networks/ip-ranges/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryIpRangeReply.class
)
public class APIQueryIpRangeMsg extends APIQueryMessage {

}
