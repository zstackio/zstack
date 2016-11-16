package org.zstack.header.message;

import org.zstack.header.errorcode.ErrorCode;

public class MessageReply extends Message {
    /**
     * @desc indicate the failure or success. Client should evaluate this field before evaluating
     * inventory field
     * @choices
     * - true
     * - false
     */
    private boolean success = true;
    /**
     * @desc indicate the reason of api failure. It presents only if success = false
     * @nullable
     */
    @NoJsonSchema
    private ErrorCode error;

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
        this.error = error;
        this.setSuccess(false);
    }

    public <T> T castReply() {
        return (T) this;
    }
}
