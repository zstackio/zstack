package org.zstack.network.hostNetwork.lldp.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetwork.lldp.LldpConstant;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryHostNetworkInterfaceLldpRefReply.class, inventoryClass = HostNetworkInterfaceLldpRefInventory.class)
@Action(category = LldpConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/lldp/info",
        optionalPaths = {"/lldp/info/{interfaceUuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryHostNetworkInterfaceLldpRefReply.class
)
public class APIQueryHostNetworkInterfaceLldpRefMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }

}