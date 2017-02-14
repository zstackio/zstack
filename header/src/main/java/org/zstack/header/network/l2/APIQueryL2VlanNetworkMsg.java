package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryL2VlanNetworkReply.class, inventoryClass = L2VlanNetworkInventory.class)
@RestRequest(
        path = "/l2-vlan-networks",
        optionalPaths = {"/l2-vlan-networks/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryL2VlanNetworkReply.class,
        parameterName = "null"
)
public class APIQueryL2VlanNetworkMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList();
    }

}
