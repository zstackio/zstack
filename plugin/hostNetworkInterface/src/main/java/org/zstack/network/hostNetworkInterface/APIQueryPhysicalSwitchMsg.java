package org.zstack.network.hostNetworkInterface;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostConstant;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
@AutoQuery(replyClass = APIQueryPhysicalSwitchReply.class, inventoryClass = PhysicalSwitchInventory.class)
@Action(category = HostConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/topo/physical-switches",
        optionalPaths = {"/topo/physical-switches/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPhysicalSwitchReply.class
)
public class APIQueryPhysicalSwitchMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
