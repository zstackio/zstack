package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQuerySystemTagReply.class, inventoryClass = SystemTagInventory.class)
@Action(category = TagConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/system-tags",
        optionalPaths = {"/system-tags/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySystemTagReply.class
)
public class APIQuerySystemTagMsg extends APIQueryMessage {
}
