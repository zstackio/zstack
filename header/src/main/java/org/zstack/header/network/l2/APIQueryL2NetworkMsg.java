package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryL2NetworkReply.class, inventoryClass = L2NetworkInventory.class)
@Action(category = L2NetworkConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/l2-networks",
        optionalPaths = {"/l2-networks/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryL2NetworkReply.class,
        parameterName = "null"
)
public class APIQueryL2NetworkMsg extends APIQueryMessage {

}
