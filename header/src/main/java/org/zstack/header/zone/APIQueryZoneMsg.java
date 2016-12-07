package org.zstack.header.zone;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryZoneReply.class, inventoryClass = ZoneInventory.class)
@RestRequest(
        path = "/zones",
        optionalPaths = {"/zones/{uuid}"},
        responseClass = APIQueryZoneReply.class,
        method = HttpMethod.GET
)
public class APIQueryZoneMsg extends APIQueryMessage {

}
