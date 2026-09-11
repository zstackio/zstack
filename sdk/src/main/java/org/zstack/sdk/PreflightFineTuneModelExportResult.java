package org.zstack.sdk;

import org.zstack.sdk.FineTuneExportPreflightResult;

public class PreflightFineTuneModelExportResult {
    public FineTuneExportPreflightResult inventory;
    public void setInventory(FineTuneExportPreflightResult inventory) {
        this.inventory = inventory;
    }
    public FineTuneExportPreflightResult getInventory() {
        return this.inventory;
    }

}
