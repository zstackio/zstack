package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryServerPoolReply.class, inventoryClass = ServerPoolInventory.class)
@RestRequest(path = "/server-pools", optionalPaths = {"/server-pools/{uuid}"}, responseClass = APIQueryServerPoolReply.class, method = HttpMethod.GET)
@Action(adminOnly = true, category = PhysicalServerConstant.SERVER_POOL_ACTION_CATEGORY, names = {"read"})
public class APIQueryServerPoolMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=pool-rack-A1");
    }
}
