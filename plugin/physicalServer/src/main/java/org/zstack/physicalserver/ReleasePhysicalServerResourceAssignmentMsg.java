package org.zstack.physicalserver;

import org.zstack.header.message.NeedReplyMessage;

public class ReleasePhysicalServerResourceAssignmentMsg extends NeedReplyMessage
        implements PhysicalServerMessage {
    private String serverUuid;
    private String roleType;
    private String consumerUuid;
    private boolean force;

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

    public String getConsumerUuid() {
        return consumerUuid;
    }

    public void setConsumerUuid(String consumerUuid) {
        this.consumerUuid = consumerUuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
