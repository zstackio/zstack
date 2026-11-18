package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class RefreshPhysicalServerResourceAssignmentResult  {

    public java.lang.String serverUuid;
    public void setServerUuid(java.lang.String serverUuid) {
        this.serverUuid = serverUuid;
    }
    public java.lang.String getServerUuid() {
        return this.serverUuid;
    }

    public boolean success;
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return this.success;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

}
