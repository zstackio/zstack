package org.zstack.portal.managementnode;

import org.zstack.header.message.NeedReplyMessage;

public class CollectManagementNodeCpuTopologyMsg extends NeedReplyMessage {
    private String serverUuid;

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }
}
