package org.zstack.physicalserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryPhysicalServerReply.class, inventoryClass = PhysicalServerInventory.class)
@Action(category = PhysicalServerConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/physical-servers",
        optionalPaths = {"/physical-servers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPhysicalServerReply.class
)
public class APIQueryPhysicalServerMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }
}
