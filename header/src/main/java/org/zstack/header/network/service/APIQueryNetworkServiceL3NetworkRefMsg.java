package org.zstack.header.network.service;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryNetworkServiceL3NetworkRefReply.class, inventoryClass = NetworkServiceL3NetworkRefInventory.class)
public class APIQueryNetworkServiceL3NetworkRefMsg extends APIQueryMessage {

}
