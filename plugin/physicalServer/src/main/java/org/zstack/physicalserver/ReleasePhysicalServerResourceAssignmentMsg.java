package org.zstack.physicalserver;

import org.zstack.header.message.NeedReplyMessage;

public class ReleasePhysicalServerResourceAssignmentMsg extends NeedReplyMessage implements PhysicalServerMessage {
    private String serverUuid;
    private String roleType;
    private Operation operation = Operation.RELEASE;

    public enum Operation {
        RELEASE, FORCE_RELEASE
    }

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

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }
}
