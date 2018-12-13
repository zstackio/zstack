package org.zstack.core.config.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/resource-configurations", method = HttpMethod.GET, responseClass = APIQueryResourceConfigReply.class)
@AutoQuery(replyClass = APIQueryResourceConfigReply.class, inventoryClass = ResourceConfigInventory.class)
public class APIQueryResourceConfigMsg extends APIQueryMessage {
}
