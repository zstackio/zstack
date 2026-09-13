package org.zstack.sdk;



public class ExecutionStageInventory  {

    public java.lang.String stageUuid;
    public void setStageUuid(java.lang.String stageUuid) {
        this.stageUuid = stageUuid;
    }
    public java.lang.String getStageUuid() {
        return this.stageUuid;
    }

    public java.lang.String parentStageUuid;
    public void setParentStageUuid(java.lang.String parentStageUuid) {
        this.parentStageUuid = parentStageUuid;
    }
    public java.lang.String getParentStageUuid() {
        return this.parentStageUuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String kind;
    public void setKind(java.lang.String kind) {
        this.kind = kind;
    }
    public java.lang.String getKind() {
        return this.kind;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String nodeUuid;
    public void setNodeUuid(java.lang.String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }
    public java.lang.String getNodeUuid() {
        return this.nodeUuid;
    }

    public java.lang.String waitingOn;
    public void setWaitingOn(java.lang.String waitingOn) {
        this.waitingOn = waitingOn;
    }
    public java.lang.String getWaitingOn() {
        return this.waitingOn;
    }

    public java.lang.String waitingReason;
    public void setWaitingReason(java.lang.String waitingReason) {
        this.waitingReason = waitingReason;
    }
    public java.lang.String getWaitingReason() {
        return this.waitingReason;
    }

    public java.sql.Timestamp startedAt;
    public void setStartedAt(java.sql.Timestamp startedAt) {
        this.startedAt = startedAt;
    }
    public java.sql.Timestamp getStartedAt() {
        return this.startedAt;
    }

    public java.sql.Timestamp finishedAt;
    public void setFinishedAt(java.sql.Timestamp finishedAt) {
        this.finishedAt = finishedAt;
    }
    public java.sql.Timestamp getFinishedAt() {
        return this.finishedAt;
    }

    public java.lang.Long elapsedMs;
    public void setElapsedMs(java.lang.Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }
    public java.lang.Long getElapsedMs() {
        return this.elapsedMs;
    }

    public java.lang.Boolean noReply;
    public void setNoReply(java.lang.Boolean noReply) {
        this.noReply = noReply;
    }
    public java.lang.Boolean getNoReply() {
        return this.noReply;
    }

    public java.lang.String httpMethod;
    public void setHttpMethod(java.lang.String httpMethod) {
        this.httpMethod = httpMethod;
    }
    public java.lang.String getHttpMethod() {
        return this.httpMethod;
    }

    public java.lang.String httpUrl;
    public void setHttpUrl(java.lang.String httpUrl) {
        this.httpUrl = httpUrl;
    }
    public java.lang.String getHttpUrl() {
        return this.httpUrl;
    }

    public java.lang.String httpTaskUuid;
    public void setHttpTaskUuid(java.lang.String httpTaskUuid) {
        this.httpTaskUuid = httpTaskUuid;
    }
    public java.lang.String getHttpTaskUuid() {
        return this.httpTaskUuid;
    }

    public java.lang.Integer httpStatusCode;
    public void setHttpStatusCode(java.lang.Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }
    public java.lang.Integer getHttpStatusCode() {
        return this.httpStatusCode;
    }

    public java.lang.Long httpElapsedMs;
    public void setHttpElapsedMs(java.lang.Long httpElapsedMs) {
        this.httpElapsedMs = httpElapsedMs;
    }
    public java.lang.Long getHttpElapsedMs() {
        return this.httpElapsedMs;
    }

}
