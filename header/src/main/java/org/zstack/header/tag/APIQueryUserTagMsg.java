package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.APIQueryUserReply;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQueryUserTagReply.class, inventoryClass = UserTagInventory.class)
@Action(category = TagConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/user-tags",
        optionalPaths = {"/user-tags/{uuid}"},
        responseClass = APIQueryUserReply.class,
        method = HttpMethod.GET
)
public class APIQueryUserTagMsg extends APIQueryMessage {
}
