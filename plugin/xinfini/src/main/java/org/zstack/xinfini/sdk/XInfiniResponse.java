package org.zstack.xinfini.sdk;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.ErrorCode;

import static org.zstack.core.Platform.operr;

public class XInfiniResponse {
    protected String message;
    protected int returnCode;

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return StringUtils.isEmpty(message);
    }

    public ErrorCode getError() {
        return operr(message);
    }

    public boolean resourceIsDeleted() {
        return returnCode == 404;
    }
}
