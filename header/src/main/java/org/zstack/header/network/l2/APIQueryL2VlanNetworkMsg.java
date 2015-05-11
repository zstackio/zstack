package org.zstack.header.network.l2;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryL2VlanNetworkReply.class, inventoryClass = L2VlanNetworkInventory.class)
public class APIQueryL2VlanNetworkMsg extends APIQueryMessage {

}
