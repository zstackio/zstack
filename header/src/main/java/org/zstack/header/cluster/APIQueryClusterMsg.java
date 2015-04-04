package org.zstack.header.cluster;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryClusterReply.class, inventoryClass = ClusterInventory.class)
public class APIQueryClusterMsg extends APIQueryMessage {

}
