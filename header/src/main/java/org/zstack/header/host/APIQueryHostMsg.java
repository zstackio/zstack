package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@Action(category = HostConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryHostReply.class, inventoryClass = HostInventory.class)
@RestRequest(
        path = "/hosts",
        optionalPaths = {"/hosts/{uuid}"},
        responseClass = APIQueryHostReply.class,
        method = HttpMethod.GET
)
public class APIQueryHostMsg extends APIQueryMessage {

}
