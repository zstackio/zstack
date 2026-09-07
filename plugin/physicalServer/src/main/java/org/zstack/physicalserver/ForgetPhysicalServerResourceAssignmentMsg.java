package org.zstack.physicalserver;

import org.zstack.header.message.NeedReplyMessage;

public class ForgetPhysicalServerResourceAssignmentMsg extends NeedReplyMessage implements PhysicalServerMessage {
    private String serverUuid;
    private String roleType;

    @Override
    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }
}
