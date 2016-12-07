package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 2/23/2016.
 */
@AutoQuery(replyClass = APIQuerySharedResourceReply.class, inventoryClass = SharedResourceInventory.class)
@RestRequest(
        path = "/accounts/resources",
        method = HttpMethod.GET,
        responseClass = APIQuerySharedResourceReply.class
)
public class APIQuerySharedResourceMsg extends APIQueryMessage {
}
