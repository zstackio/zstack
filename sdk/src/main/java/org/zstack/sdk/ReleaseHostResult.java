package org.zstack.sdk;

import org.zstack.sdk.HostInventory;
import org.zstack.sdk.ErrorCode;

public class ReleaseHostResult {
    public java.lang.Boolean success;
    public void setSuccess(java.lang.Boolean success) {
        this.success = success;
    }
    public java.lang.Boolean getSuccess() {
        return this.success;
    }

    public HostInventory inventory;
    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }
    public HostInventory getInventory() {
        return this.inventory;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

}
