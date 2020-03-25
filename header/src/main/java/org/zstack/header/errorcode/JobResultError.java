package org.zstack.header.errorcode;

import org.zstack.utils.string.ErrorCodeElaboration;

/**
 * Created by mingjian.deng on 2020/3/25.
 */
public class JobResultError {
    private ErrorCodeElaboration message;
    private String detail;

    public JobResultError(ErrorCodeElaboration message, String detail) {
        this.message = message;
        this.detail = detail;
    }

    public ErrorCodeElaboration getMessage() {
        return message;
    }

    public void setMessage(ErrorCodeElaboration message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
