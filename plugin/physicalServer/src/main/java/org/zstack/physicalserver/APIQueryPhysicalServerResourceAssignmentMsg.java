package org.zstack.physicalserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(
        replyClass = APIQueryPhysicalServerResourceAssignmentReply.class,
        inventoryClass = PhysicalServerResourceAssignmentInventory.class
)
@Action(category = PhysicalServerConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/physical-servers/resource-assignments",
        optionalPaths = {"/physical-servers/resource-assignments/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPhysicalServerResourceAssignmentReply.class
)
public class APIQueryPhysicalServerResourceAssignmentMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("serverUuid=" + uuid(), "roleType=COMPUTE");
    }
}
