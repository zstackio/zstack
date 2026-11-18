package org.zstack.physicalserver;

import org.zstack.header.errorcode.ErrorCode;

public class RefreshPhysicalServerResourceAssignmentResult {
    private String serverUuid;
    private boolean success = true;
    private ErrorCode error;

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.success = false;
        this.error = error;
    }
}
