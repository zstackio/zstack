package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/14/2015.
 */
@AutoQuery(replyClass = APIQueryAccountReply.class, inventoryClass = AccountInventory.class)
@RestRequest(
        path = "/accounts",
        optionalPaths = {"/accounts/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAccountReply.class
)
public class APIQueryAccountMsg extends APIQueryMessage {
}
