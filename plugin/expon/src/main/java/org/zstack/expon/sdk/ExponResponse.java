package org.zstack.expon.sdk;

import org.zstack.header.errorcode.ErrorCode;

import static org.zstack.core.Platform.operr;

public class ExponResponse {
    protected String retCode;
    protected String message;

    public String getRetCode() {
        return retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return "0".equals(retCode);
    }

    public ErrorCode getError() {
        return operr(message);
    }
}
