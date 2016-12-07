package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/14/2015.
 */
@AutoQuery(replyClass = APIQueryUserReply.class, inventoryClass = UserInventory.class)
@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/accounts/users",
        optionalPaths = {"/accounts/users/{uuid}"},
        responseClass = APIQueryUserReply.class,
        method = HttpMethod.GET
)
public class APIQueryUserMsg extends APIQueryMessage {
}
