package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryL3NetworkReply.class, inventoryClass = L3NetworkInventory.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/l3-networks",
        optionalPaths = {"/l3-networks/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryL3NetworkReply.class
)
public class APIQueryL3NetworkMsg extends APIQueryMessage {

}
