package org.zstack.header.apimediator;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/management-nodes/ready",
        method = HttpMethod.GET,
        parameterName = "params",
        responseClass = APIIsReadyToGoReply.class
)
public class APIIsReadyToGoMsg extends APISyncCallMessage {
    private String managementNodeId;

    public String getManagementNodeId() {
        return managementNodeId;
    }

    public void setManagementNodeId(String managementNodeId) {
        this.managementNodeId = managementNodeId;
    }

    public APIIsReadyToGoMsg() {
    }
}
