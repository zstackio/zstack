package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQueryManagementNodeReply.class, inventoryClass = ManagementNodeInventory.class)
@RestRequest(
        path = "/management-nodes",
        optionalPaths = {"/management-nodes/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryManagementNodeReply.class
)
public class APIQueryManagementNodeMsg extends APIQueryMessage {
}
