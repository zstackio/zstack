package org.zstack.sdk;

import org.zstack.sdk.ExecutionTriggerInventory;

public class ExecutionInventory  {

    public java.lang.String executionUuid;
    public void setExecutionUuid(java.lang.String executionUuid) {
        this.executionUuid = executionUuid;
    }
    public java.lang.String getExecutionUuid() {
        return this.executionUuid;
    }

    public java.lang.String apiUuid;
    public void setApiUuid(java.lang.String apiUuid) {
        this.apiUuid = apiUuid;
    }
    public java.lang.String getApiUuid() {
        return this.apiUuid;
    }

    public java.lang.String messageUuid;
    public void setMessageUuid(java.lang.String messageUuid) {
        this.messageUuid = messageUuid;
    }
    public java.lang.String getMessageUuid() {
        return this.messageUuid;
    }

    public java.lang.String taskRunUuid;
    public void setTaskRunUuid(java.lang.String taskRunUuid) {
        this.taskRunUuid = taskRunUuid;
    }
    public java.lang.String getTaskRunUuid() {
        return this.taskRunUuid;
    }

    public java.lang.String rootMessageUuid;
    public void setRootMessageUuid(java.lang.String rootMessageUuid) {
        this.rootMessageUuid = rootMessageUuid;
    }
    public java.lang.String getRootMessageUuid() {
        return this.rootMessageUuid;
    }

    public java.lang.String nodeUuid;
    public void setNodeUuid(java.lang.String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }
    public java.lang.String getNodeUuid() {
        return this.nodeUuid;
    }

    public java.lang.String operationId;
    public void setOperationId(java.lang.String operationId) {
        this.operationId = operationId;
    }
    public java.lang.String getOperationId() {
        return this.operationId;
    }

    public java.lang.String traceId;
    public void setTraceId(java.lang.String traceId) {
        this.traceId = traceId;
    }
    public java.lang.String getTraceId() {
        return this.traceId;
    }

    public java.lang.String apiName;
    public void setApiName(java.lang.String apiName) {
        this.apiName = apiName;
    }
    public java.lang.String getApiName() {
        return this.apiName;
    }

    public java.lang.String requestKind;
    public void setRequestKind(java.lang.String requestKind) {
        this.requestKind = requestKind;
    }
    public java.lang.String getRequestKind() {
        return this.requestKind;
    }

    public java.lang.Boolean noReply;
    public void setNoReply(java.lang.Boolean noReply) {
        this.noReply = noReply;
    }
    public java.lang.Boolean getNoReply() {
        return this.noReply;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public ExecutionTriggerInventory trigger;
    public void setTrigger(ExecutionTriggerInventory trigger) {
        this.trigger = trigger;
    }
    public ExecutionTriggerInventory getTrigger() {
        return this.trigger;
    }

    public java.sql.Timestamp acceptedAt;
    public void setAcceptedAt(java.sql.Timestamp acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
    public java.sql.Timestamp getAcceptedAt() {
        return this.acceptedAt;
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

    public java.sql.Timestamp lastHeartbeatAt;
    public void setLastHeartbeatAt(java.sql.Timestamp lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }
    public java.sql.Timestamp getLastHeartbeatAt() {
        return this.lastHeartbeatAt;
    }

    public java.sql.Timestamp observedAt;
    public void setObservedAt(java.sql.Timestamp observedAt) {
        this.observedAt = observedAt;
    }
    public java.sql.Timestamp getObservedAt() {
        return this.observedAt;
    }

    public java.lang.Long elapsedMs;
    public void setElapsedMs(java.lang.Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }
    public java.lang.Long getElapsedMs() {
        return this.elapsedMs;
    }

    public java.lang.Long queueWaitMs;
    public void setQueueWaitMs(java.lang.Long queueWaitMs) {
        this.queueWaitMs = queueWaitMs;
    }
    public java.lang.Long getQueueWaitMs() {
        return this.queueWaitMs;
    }

    public java.lang.Long executionMs;
    public void setExecutionMs(java.lang.Long executionMs) {
        this.executionMs = executionMs;
    }
    public java.lang.Long getExecutionMs() {
        return this.executionMs;
    }

    public java.lang.Long downstreamWaitMs;
    public void setDownstreamWaitMs(java.lang.Long downstreamWaitMs) {
        this.downstreamWaitMs = downstreamWaitMs;
    }
    public java.lang.Long getDownstreamWaitMs() {
        return this.downstreamWaitMs;
    }

    public java.util.List activeStages;
    public void setActiveStages(java.util.List activeStages) {
        this.activeStages = activeStages;
    }
    public java.util.List getActiveStages() {
        return this.activeStages;
    }

    public java.util.List events;
    public void setEvents(java.util.List events) {
        this.events = events;
    }
    public java.util.List getEvents() {
        return this.events;
    }

    public java.lang.Boolean partial;
    public void setPartial(java.lang.Boolean partial) {
        this.partial = partial;
    }
    public java.lang.Boolean getPartial() {
        return this.partial;
    }

    public java.lang.String partialReason;
    public void setPartialReason(java.lang.String partialReason) {
        this.partialReason = partialReason;
    }
    public java.lang.String getPartialReason() {
        return this.partialReason;
    }

    public java.util.List sourceNodes;
    public void setSourceNodes(java.util.List sourceNodes) {
        this.sourceNodes = sourceNodes;
    }
    public java.util.List getSourceNodes() {
        return this.sourceNodes;
    }

    public java.lang.String visibility;
    public void setVisibility(java.lang.String visibility) {
        this.visibility = visibility;
    }
    public java.lang.String getVisibility() {
        return this.visibility;
    }

    public java.lang.Integer attempt;
    public void setAttempt(java.lang.Integer attempt) {
        this.attempt = attempt;
    }
    public java.lang.Integer getAttempt() {
        return this.attempt;
    }

    public java.lang.String error;
    public void setError(java.lang.String error) {
        this.error = error;
    }
    public java.lang.String getError() {
        return this.error;
    }

    public java.sql.Timestamp expiresAt;
    public void setExpiresAt(java.sql.Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }
    public java.sql.Timestamp getExpiresAt() {
        return this.expiresAt;
    }

    public java.lang.Boolean truncated;
    public void setTruncated(java.lang.Boolean truncated) {
        this.truncated = truncated;
    }
    public java.lang.Boolean getTruncated() {
        return this.truncated;
    }

}
