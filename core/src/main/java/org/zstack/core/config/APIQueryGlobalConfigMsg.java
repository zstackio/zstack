package org.zstack.core.config;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@AutoQuery(replyClass = APIQueryGlobalConfigReply.class, inventoryClass = GlobalConfigInventory.class)
@Action(category = "configuration", names = {"read"})
@RestRequest(
        path = "/global-configurations",
        method = HttpMethod.GET,
        responseClass = APIQueryGlobalConfigReply.class
)
public class APIQueryGlobalConfigMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }

}
