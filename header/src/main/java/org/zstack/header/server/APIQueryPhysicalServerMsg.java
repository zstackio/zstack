package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryPhysicalServerReply.class, inventoryClass = PhysicalServerInventory.class)
@RestRequest(
        path = "/physical-servers",
        optionalPaths = {"/physical-servers/{uuid}"},
        responseClass = APIQueryPhysicalServerReply.class,
        method = HttpMethod.GET
)
@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryPhysicalServerMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("name=server1", "state=Enabled");
    }
}
