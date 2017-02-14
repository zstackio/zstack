package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.APICreateInstanceOfferingEvent;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVirtualRouterOfferingReply.class, inventoryClass = VirtualRouterOfferingInventory.class)
@Action(category = VirtualRouterConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/instance-offerings/virtual-routers",
        optionalPaths = {"/instance-offerings/virtual-routers/{uuid}"},
        responseClass = APIQueryVirtualRouterOfferingReply.class,
        method = HttpMethod.GET
)
public class APIQueryVirtualRouterOfferingMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList();
    }

}
