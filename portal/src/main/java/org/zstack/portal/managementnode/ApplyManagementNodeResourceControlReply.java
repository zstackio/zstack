package org.zstack.portal.managementnode;

import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.ResourceControlResponse;

public class ApplyManagementNodeResourceControlReply extends MessageReply {
    private ResourceControlResponse response;

    public ResourceControlResponse getResponse() {
        return response;
    }

    public void setResponse(ResourceControlResponse response) {
        this.response = response;
    }
}
