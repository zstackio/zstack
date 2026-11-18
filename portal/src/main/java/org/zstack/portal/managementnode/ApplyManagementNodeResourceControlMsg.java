package org.zstack.portal.managementnode;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.physicalserver.ResourceControlCommand;

public class ApplyManagementNodeResourceControlMsg extends NeedReplyMessage {
    private String serverUuid;
    private ResourceControlCommand command;

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public ResourceControlCommand getCommand() {
        return command;
    }

    public void setCommand(ResourceControlCommand command) {
        this.command = command;
    }
}
