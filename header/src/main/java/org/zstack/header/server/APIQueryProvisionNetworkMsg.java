package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryProvisionNetworkReply.class, inventoryClass = PhysicalServerProvisionNetworkInventory.class)
@RestRequest(path = "/provision-networks", optionalPaths = {"/provision-networks/{uuid}"}, responseClass = APIQueryProvisionNetworkReply.class, method = HttpMethod.GET)
@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryProvisionNetworkMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=provision-net-1");
    }
}
