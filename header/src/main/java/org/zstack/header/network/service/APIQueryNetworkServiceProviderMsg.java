package org.zstack.header.network.service;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryNetworkServiceProviderReply.class, inventoryClass = NetworkServiceProviderInventory.class)
public class APIQueryNetworkServiceProviderMsg extends APIQueryMessage {

}
