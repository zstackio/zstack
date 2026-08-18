package org.zstack.header.network;

import org.zstack.header.message.NeedReplyMessage;

public class CompleteZnsSegmentMutationMsg extends NeedReplyMessage {
    private String controllerUuid;
    private String relationUuid;
    private String operationUuid;
    private long acceptedConfigVersion;
    private boolean success;
    private String errorCode;
    private String errorDetails;

    public String getControllerUuid() {
        return controllerUuid;
    }

    public void setControllerUuid(String controllerUuid) {
        this.controllerUuid = controllerUuid;
    }

    public String getRelationUuid() {
        return relationUuid;
    }

    public void setRelationUuid(String relationUuid) {
        this.relationUuid = relationUuid;
    }

    public String getOperationUuid() {
        return operationUuid;
    }

    public void setOperationUuid(String operationUuid) {
        this.operationUuid = operationUuid;
    }

    public long getAcceptedConfigVersion() {
        return acceptedConfigVersion;
    }

    public void setAcceptedConfigVersion(long acceptedConfigVersion) {
        this.acceptedConfigVersion = acceptedConfigVersion;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
