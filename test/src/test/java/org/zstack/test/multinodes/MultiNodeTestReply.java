package org.zstack.test.multinodes;

import org.zstack.header.message.MessageReply;

/**
 */
public class MultiNodeTestReply extends MessageReply {
    private String managementNodeId;

    public String getManagementNodeId() {
        return managementNodeId;
    }

    public void setManagementNodeId(String managementNodeId) {
        this.managementNodeId = managementNodeId;
    }
}
