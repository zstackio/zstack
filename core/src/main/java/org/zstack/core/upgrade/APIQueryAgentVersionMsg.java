package org.zstack.core.upgrade;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;


@AutoQuery(replyClass = APIQueryAgentVersionReply.class, inventoryClass = AgentVersionInventory.class)
@RestRequest(
        path = "/agent-version",
        optionalPaths = {"/agent-version/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAgentVersionReply.class
)
public class APIQueryAgentVersionMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.emptyList();
    }
}
