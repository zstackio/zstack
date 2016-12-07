package org.zstack.header.apimediator;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIIsReadyToGoReply extends APIReply {
    private String managementNodeId;

    public APIIsReadyToGoReply() {
    }

    public void setManagementNodeId(String managementNodeId) {
        this.managementNodeId = managementNodeId;
    }

    public String getManagementNodeId() {
        return managementNodeId;
    }
}
