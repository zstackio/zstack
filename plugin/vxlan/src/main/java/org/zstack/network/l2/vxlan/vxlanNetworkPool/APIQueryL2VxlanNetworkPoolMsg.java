package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 15/03/2017.
 */
@AutoQuery(replyClass = APIQueryL2VxlanNetworkPoolReply.class, inventoryClass = L2VxlanNetworkPoolInventory.class)
@Action(category = VxlanNetworkPoolConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/l2-networks/vxlan-pool",
        optionalPaths = {"/l2-networks/vxlan-pool/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryL2VxlanNetworkPoolReply.class,
        parameterName = "null"
)
public class APIQueryL2VxlanNetworkPoolMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
