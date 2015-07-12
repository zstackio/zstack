package org.zstack.header.network.service;

import org.zstack.header.identity.Action;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryNetworkServiceL3NetworkRefReply.class, inventoryClass = NetworkServiceL3NetworkRefInventory.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryNetworkServiceL3NetworkRefMsg extends APIQueryMessage {

}
