package org.zstack.header.cluster;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;

@AutoQuery(replyClass = APIQueryClusterReply.class, inventoryClass = ClusterInventory.class)
@Action(category = ClusterConstant.CATEGORY, names = {"read"})
public class APIQueryClusterMsg extends APIQueryMessage {

}
