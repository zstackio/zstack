package org.zstack.header.core.execution;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;
import java.sql.Timestamp;

@PythonClassInventory
public class ExecutionEventInventory implements Serializable {
    private Long sequence;
    private Timestamp timestamp;
    private String type;
    private String stageUuid;
    private String stageName;
    private String stageKind;
    private String nodeUuid;
    private String messageUuid;
    private String parentStageUuid;
    private String details;
    private Boolean noReply;
    private String httpMethod;
    private String httpUrl;
    private String httpTaskUuid;
    private Integer httpStatusCode;
    private Long httpElapsedMs;

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStageUuid() {
        return stageUuid;
    }

    public void setStageUuid(String stageUuid) {
        this.stageUuid = stageUuid;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getStageKind() {
        return stageKind;
    }

    public void setStageKind(String stageKind) {
        this.stageKind = stageKind;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    public String getMessageUuid() {
        return messageUuid;
    }

    public void setMessageUuid(String messageUuid) {
        this.messageUuid = messageUuid;
    }

    public String getParentStageUuid() {
        return parentStageUuid;
    }

    public void setParentStageUuid(String parentStageUuid) {
        this.parentStageUuid = parentStageUuid;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Boolean getNoReply() {
        return noReply;
    }

    public void setNoReply(Boolean noReply) {
        this.noReply = noReply;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public String getHttpTaskUuid() {
        return httpTaskUuid;
    }

    public void setHttpTaskUuid(String httpTaskUuid) {
        this.httpTaskUuid = httpTaskUuid;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public Long getHttpElapsedMs() {
        return httpElapsedMs;
    }

    public void setHttpElapsedMs(Long httpElapsedMs) {
        this.httpElapsedMs = httpElapsedMs;
    }
}
