package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQueryEipReply.class, inventoryClass = EipInventory.class)
@Action(category = EipConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/eips",
        optionalPaths = {"/eips/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryEipReply.class
)
public class APIQueryEipMsg extends APIQueryMessage {
}
