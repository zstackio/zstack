package org.zstack.header.zone;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryZoneReply.class, inventoryClass = ZoneInventory.class)
@RestRequest(
        path = "/zones",
        optionalPaths = {"/zones/{uuid}"},
        responseClass = APIQueryZoneReply.class,
        method = HttpMethod.GET
)
@Action(category = "zone", names = {"read"})
public class APIQueryZoneMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("name=TestZone", "state=Enabled");
    }
}
