package org.zstack.abstraction.sns;

import java.util.ArrayList;
import java.util.List;

public class PluginEndpointSendResult {
    private boolean success;
    private String driverErrorCode;
    private String errorMessage;
    private List<String> failedTargets = new ArrayList<>();

    public static PluginEndpointSendResult from(boolean success) {
        PluginEndpointSendResult result = new PluginEndpointSendResult();
        result.setSuccess(success);
        return result;
    }

    public static PluginEndpointSendResult failure(String driverErrorCode, String errorMessage,
                                                   List<String> failedTargets) {
        PluginEndpointSendResult result = from(false);
        result.setDriverErrorCode(driverErrorCode);
        result.setErrorMessage(errorMessage);
        result.setFailedTargets(failedTargets);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDriverErrorCode() {
        return driverErrorCode;
    }

    public void setDriverErrorCode(String driverErrorCode) {
        this.driverErrorCode = driverErrorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getFailedTargets() {
        return failedTargets;
    }

    public void setFailedTargets(List<String> failedTargets) {
        this.failedTargets = failedTargets == null ? new ArrayList<>() : new ArrayList<>(failedTargets);
    }
}
