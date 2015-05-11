package org.zstack.header.apimediator;

import org.zstack.header.message.APISyncCallMessage;

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
