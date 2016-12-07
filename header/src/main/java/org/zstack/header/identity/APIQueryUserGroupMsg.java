package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/14/2015.
 */

@AutoQuery(replyClass = APIQueryUserGroupReply.class, inventoryClass = UserGroupInventory.class)
@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/accounts/groups",
        optionalPaths = "/accounts/groups/{uuid}",
        method = HttpMethod.GET,
        responseClass = APIQueryUserGroupReply.class
)
public class APIQueryUserGroupMsg extends APIQueryMessage {
}
