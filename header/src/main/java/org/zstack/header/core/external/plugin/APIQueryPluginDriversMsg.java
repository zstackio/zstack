package org.zstack.header.core.external.plugin;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryPluginDriversReply.class, inventoryClass = PluginDriverInventory.class)
@RestRequest(
        path = "/external/plugins",
        method = HttpMethod.GET,
        responseClass = APIQueryPluginDriversReply.class
)
public class APIQueryPluginDriversMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=test");
    }
}
