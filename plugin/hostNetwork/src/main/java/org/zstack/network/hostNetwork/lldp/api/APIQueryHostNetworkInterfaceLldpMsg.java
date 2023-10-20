package org.zstack.network.hostNetwork.lldp.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory;
import org.zstack.network.hostNetwork.lldp.LldpConstant;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryHostNetworkInterfaceLldpReply.class, inventoryClass = HostNetworkInterfaceLldpRefInventory.class)
@Action(category = LldpConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/lldp/all",
        optionalPaths = {"/lldp/{interfaceUuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryHostNetworkInterfaceLldpReply.class
)
public class APIQueryHostNetworkInterfaceLldpMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }

}