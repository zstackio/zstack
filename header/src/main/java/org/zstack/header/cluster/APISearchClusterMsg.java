package org.zstack.header.cluster;

import org.zstack.header.identity.Action;
import org.zstack.header.search.APISearchMessage;

@Action(category = ClusterConstant.CATEGORY, names = {"read"})
public class APISearchClusterMsg extends APISearchMessage {
}
