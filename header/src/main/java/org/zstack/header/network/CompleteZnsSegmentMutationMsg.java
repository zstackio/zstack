package org.zstack.header.network;

import org.zstack.header.message.NeedReplyMessage;

public class CompleteZnsSegmentMutationMsg extends NeedReplyMessage {
    private String resourceUuid;
    private String operationUuid;
    private boolean success;
    private String errorCode;
    private String errorDetails;

    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }
    public String getOperationUuid() { return operationUuid; }
    public void setOperationUuid(String value) { operationUuid = value; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean value) { success = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { errorCode = value; }
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String value) { errorDetails = value; }
}
