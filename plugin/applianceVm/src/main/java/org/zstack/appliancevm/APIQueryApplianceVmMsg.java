package org.zstack.appliancevm;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

/**
 */
@AutoQuery(replyClass = APIQueryApplianceVmReply.class, inventoryClass = ApplianceVmInventory.class)
@RestRequest(
        path = "/vm-instances/appliances",
        optionalPaths = {"/vm-instances/appliances/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryApplianceVmReply.class
)
public class APIQueryApplianceVmMsg extends APIQueryMessage {
}
