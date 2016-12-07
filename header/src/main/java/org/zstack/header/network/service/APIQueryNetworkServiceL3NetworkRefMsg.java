package org.zstack.header.network.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@AutoQuery(replyClass = APIQueryNetworkServiceL3NetworkRefReply.class, inventoryClass = NetworkServiceL3NetworkRefInventory.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/l3-networks/network-services/refs",
        method = HttpMethod.GET,
        responseClass = APIQueryNetworkServiceL3NetworkRefReply.class
)
public class APIQueryNetworkServiceL3NetworkRefMsg extends APIQueryMessage {

}
