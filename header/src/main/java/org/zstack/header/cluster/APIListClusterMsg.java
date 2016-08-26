package org.zstack.header.cluster;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIListMessage;

import java.util.List;

@Action(category = ClusterConstant.CATEGORY, names = {"read"})
public class APIListClusterMsg extends APIListMessage {
    public APIListClusterMsg() {
    }
    
    public APIListClusterMsg(List<String> uuids) {
        super(uuids);
    }
}
