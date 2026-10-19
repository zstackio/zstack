package org.zstack.sdk.network.zns;

import org.zstack.sdk.network.zns.ZnsSegmentRefInventory;

public class SyncZnsSegmentResult {
    public ZnsSegmentRefInventory inventory;
    public void setInventory(ZnsSegmentRefInventory inventory) {
        this.inventory = inventory;
    }
    public ZnsSegmentRefInventory getInventory() {
        return this.inventory;
    }

    public java.lang.String result;
    public void setResult(java.lang.String result) {
        this.result = result;
    }
    public java.lang.String getResult() {
        return this.result;
    }

    public java.lang.String stage;
    public void setStage(java.lang.String stage) {
        this.stage = stage;
    }
    public java.lang.String getStage() {
        return this.stage;
    }

}
