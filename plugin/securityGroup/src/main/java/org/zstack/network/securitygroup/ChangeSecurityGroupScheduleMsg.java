package org.zstack.network.securitygroup;

import org.zstack.header.message.NeedReplyMessage;

public class ChangeSecurityGroupScheduleMsg extends NeedReplyMessage implements SecurityGroupMessage {
    public enum Operation {
        SET,
        REFRESH
    }

    private String securityGroupUuid;
    private String scheduleUuid;
    private Operation operation;
    private boolean ignoreRefreshFailure;

    @Override
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public String getScheduleUuid() {
        return scheduleUuid;
    }

    public void setScheduleUuid(String scheduleUuid) {
        this.scheduleUuid = scheduleUuid;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public boolean isIgnoreRefreshFailure() {
        return ignoreRefreshFailure;
    }

    public void setIgnoreRefreshFailure(boolean ignoreRefreshFailure) {
        this.ignoreRefreshFailure = ignoreRefreshFailure;
    }
}
