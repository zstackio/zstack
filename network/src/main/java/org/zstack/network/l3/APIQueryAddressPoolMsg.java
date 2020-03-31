package org.zstack.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.network.l3.APIQueryIpRangeReply;
import org.zstack.header.network.l3.AddressPoolInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryAddressPoolReply.class, inventoryClass = AddressPoolInventory.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/l3-networks/address-pools",
        optionalPaths = {"/l3-networks/address-pools/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAddressPoolReply.class
)
public class APIQueryAddressPoolMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList(String.format("uuid=" + uuid()));
    }

}
