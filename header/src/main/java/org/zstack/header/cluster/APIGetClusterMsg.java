package org.zstack.header.cluster;

import org.zstack.header.identity.Action;
import org.zstack.header.search.APIGetMessage;

@Action(category = ClusterConstant.CATEGORY, names = {"read"})
public class APIGetClusterMsg extends APIGetMessage {

}
