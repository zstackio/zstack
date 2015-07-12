package org.zstack.header.network.service;

import org.zstack.header.identity.Action;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryNetworkServiceProviderReply.class, inventoryClass = NetworkServiceProviderInventory.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryNetworkServiceProviderMsg extends APIQueryMessage {

}
