package org.zstack.core.thread;

import org.zstack.header.errorcode.ErrorCode;

public class SingleFlightTaskResult {
    private boolean success = true;
    private ErrorCode errorCode;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.success = false;
        this.errorCode = errorCode;
    }
}
