package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryImageReply.class, inventoryClass = ImageInventory.class)
@Action(category = ImageConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/images",
        optionalPaths = {"/images/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryImageReply.class
)
public class APIQueryImageMsg extends APIQueryMessage {

}
