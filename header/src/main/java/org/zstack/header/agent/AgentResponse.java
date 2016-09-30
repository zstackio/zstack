package org.zstack.header.agent;

/**
 */
public class AgentResponse {
    private String error;
    private boolean success = true;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
