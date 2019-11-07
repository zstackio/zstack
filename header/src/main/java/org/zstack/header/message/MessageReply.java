package org.zstack.header.message;

import org.zstack.header.errorcode.ErrorCode;

public class MessageReply extends Message {
    /**
     * @desc indicate the failure or success. Client should evaluate this field before evaluating
     * inventory field
     * @choices - true
     * - false
     */
    private boolean success = true;
    /**
     * @desc sometimes we just cancel this msg execution, neither success or failed
     */
    private boolean canceled = false;
    /**
     * @desc indicate the reason of api failure. It presents only if success = false
     * @nullable
     */
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

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(ErrorCode error) {
        this.error = error;
        this.canceled = true;
        this.setSuccess(false);
    }

    public String getCorrelationId() {
        return getHeaderEntry("correlationId");
    }

    public <T> T castReply() {
        return (T) this;
    }
}
