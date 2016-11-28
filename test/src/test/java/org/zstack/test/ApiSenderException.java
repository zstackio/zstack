package org.zstack.test;

import org.zstack.header.errorcode.ErrorCode;

public class ApiSenderException extends Exception {
    private ErrorCode error;

    public ApiSenderException(ErrorCode error) {
        super();
        this.error = error;
    }

    @Override
    public String getMessage() {
        if (error == null) {
            return "";
        }

        return error.toString();
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
