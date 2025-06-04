package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryImageGroupRefReply.class, inventoryClass = ImageGroupRefInventory.class)
@Action(category = ImageConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/imagegrouprefs",
        optionalPaths = {"/imagegrouprefs/{imageGroupUuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryImageGroupRefReply.class
)
public class APIQueryImageGroupRefMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }

}