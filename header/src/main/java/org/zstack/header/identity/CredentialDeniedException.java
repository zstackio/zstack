package org.zstack.header.identity;

import org.zstack.header.errorcode.ErrorCode;

public class CredentialDeniedException extends Exception {
    private ErrorCode error;

    public CredentialDeniedException(ErrorCode error) {
        super();
        this.error = error;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
