package org.zstack.header.core.execution;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@PythonClassInventory
public class ExecutionInventory implements Serializable {
    private String executionUuid;
    private String apiUuid;
    private String messageUuid;
    private String taskRunUuid;
    private String rootMessageUuid;
    private String nodeUuid;
    private String operationId;
    private String traceId;
    private String apiName;
    private String requestKind;
    private Boolean noReply;
    private String state;
    private ExecutionTriggerInventory trigger;
    private Timestamp acceptedAt;
    private Timestamp startedAt;
    private Timestamp finishedAt;
    private Timestamp lastHeartbeatAt;
    private Timestamp observedAt;
    private Long elapsedMs;
    private Long queueWaitMs;
    private Long executionMs;
    private Long downstreamWaitMs;
    private List<ExecutionStageInventory> activeStages = new ArrayList<>();
    private List<ExecutionEventInventory> events = new ArrayList<>();
    private Boolean partial;
    private String partialReason;
    private List<String> sourceNodes = new ArrayList<>();
    private String visibility;
    private Integer attempt;
    private String error;
    private Timestamp expiresAt;
    private Boolean truncated;

    public String getExecutionUuid() {
        return executionUuid;
    }

    public void setExecutionUuid(String executionUuid) {
        this.executionUuid = executionUuid;
    }

    public String getApiUuid() {
        return apiUuid;
    }

    public void setApiUuid(String apiUuid) {
        this.apiUuid = apiUuid;
    }

    public String getMessageUuid() {
        return messageUuid;
    }

    public void setMessageUuid(String messageUuid) {
        this.messageUuid = messageUuid;
    }

    public String getTaskRunUuid() {
        return taskRunUuid;
    }

    public void setTaskRunUuid(String taskRunUuid) {
        this.taskRunUuid = taskRunUuid;
    }

    public String getRootMessageUuid() {
        return rootMessageUuid;
    }

    public void setRootMessageUuid(String rootMessageUuid) {
        this.rootMessageUuid = rootMessageUuid;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getRequestKind() {
        return requestKind;
    }

    public void setRequestKind(String requestKind) {
        this.requestKind = requestKind;
    }

    public Boolean getNoReply() {
        return noReply;
    }

    public void setNoReply(Boolean noReply) {
        this.noReply = noReply;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ExecutionTriggerInventory getTrigger() {
        return trigger;
    }

    public void setTrigger(ExecutionTriggerInventory trigger) {
        this.trigger = trigger;
    }

    public Timestamp getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Timestamp acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Timestamp finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Timestamp getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Timestamp lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Timestamp getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Timestamp observedAt) {
        this.observedAt = observedAt;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public Long getQueueWaitMs() {
        return queueWaitMs;
    }

    public void setQueueWaitMs(Long queueWaitMs) {
        this.queueWaitMs = queueWaitMs;
    }

    public Long getExecutionMs() {
        return executionMs;
    }

    public void setExecutionMs(Long executionMs) {
        this.executionMs = executionMs;
    }

    public Long getDownstreamWaitMs() {
        return downstreamWaitMs;
    }

    public void setDownstreamWaitMs(Long downstreamWaitMs) {
        this.downstreamWaitMs = downstreamWaitMs;
    }

    public List<ExecutionStageInventory> getActiveStages() {
        return activeStages;
    }

    public void setActiveStages(List<ExecutionStageInventory> activeStages) {
        this.activeStages = activeStages;
    }

    public List<ExecutionEventInventory> getEvents() {
        return events;
    }

    public void setEvents(List<ExecutionEventInventory> events) {
        this.events = events;
    }

    public Boolean getPartial() {
        return partial;
    }

    public void setPartial(Boolean partial) {
        this.partial = partial;
    }

    public String getPartialReason() {
        return partialReason;
    }

    public void setPartialReason(String partialReason) {
        this.partialReason = partialReason;
    }

    public List<String> getSourceNodes() {
        return sourceNodes;
    }

    public void setSourceNodes(List<String> sourceNodes) {
        this.sourceNodes = sourceNodes;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getTruncated() {
        return truncated;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }
}
