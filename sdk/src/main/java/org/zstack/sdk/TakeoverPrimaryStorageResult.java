package org.zstack.sdk;

import org.zstack.sdk.PrimaryStorageInventory;
import org.zstack.sdk.ReconnectResult;

public class TakeoverPrimaryStorageResult {
    public PrimaryStorageInventory inventory;
    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
    public PrimaryStorageInventory getInventory() {
        return this.inventory;
    }

    public ReconnectResult reconnectResult;
    public void setReconnectResult(ReconnectResult reconnectResult) {
        this.reconnectResult = reconnectResult;
    }
    public ReconnectResult getReconnectResult() {
        return this.reconnectResult;
    }

    public java.lang.String reconnectError;
    public void setReconnectError(java.lang.String reconnectError) {
        this.reconnectError = reconnectError;
    }
    public java.lang.String getReconnectError() {
        return this.reconnectError;
    }

}
