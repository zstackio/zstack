package org.zstack.header.core.execution;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.CoreConstant;
import org.zstack.header.core.NoDoc;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

@Action(category = CoreConstant.ACTION_CATEGORY, names = {"read"}, adminOnly = true)
@NoDoc
@RestRequest(
        path = "/executions",
        optionalPaths = {"/executions/{executionUuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryExecutionReply.class
)
public class APIQueryExecutionMsg extends APISyncCallMessage {
    @APIParam(required = false, nonempty = true)
    private String executionUuid;

    @APIParam(required = false, nonempty = true)
    private String apiUuid;

    @APIParam(required = false, nonempty = true)
    private String messageUuid;

    @APIParam(required = false, nonempty = true)
    private String taskRunUuid;

    @APIParam(required = false, validValues = {"API", "SCHEDULED_TASK", "MESSAGE"})
    private String triggerType;

    @APIParam(required = false, nonempty = true)
    private String triggerName;

    @APIParam(required = false, validValues = {
            "RECEIVED", "QUEUED", "RUNNING", "WAITING", "SUCCEEDED",
            "FAILED", "TIMEOUT", "CANCELLED", "UNKNOWN", "STALE", "EXPIRED"
    })
    private String state;

    @APIParam(required = false, nonempty = true)
    private String startedAfter;

    @APIParam(required = false, nonempty = true)
    private String startedBefore;

    @APIParam(required = false, nonempty = true)
    private String nodeUuid;

    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long minExecutionDurationMs;

    @APIParam(required = false, validValues = {"summary", "timeline", "criticalPath"})
    private String detail = "summary";

    @APIParam(required = false, numberRange = {1, 200})
    private Integer limit = 50;

    @APIParam(required = false, nonempty = true)
    private String cursor;

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

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStartedAfter() {
        return startedAfter;
    }

    public void setStartedAfter(String startedAfter) {
        this.startedAfter = startedAfter;
    }

    public String getStartedBefore() {
        return startedBefore;
    }

    public void setStartedBefore(String startedBefore) {
        this.startedBefore = startedBefore;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    public Long getMinExecutionDurationMs() {
        return minExecutionDurationMs;
    }

    public void setMinExecutionDurationMs(Long minExecutionDurationMs) {
        this.minExecutionDurationMs = minExecutionDurationMs;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public static List<String> __example__() {
        return Arrays.asList("apiUuid=" + uuid());
    }
}
