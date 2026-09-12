package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetTaskInventory;

public class SubmitZsDatasetDocumentProcessingTaskResult {
    public ZsDatasetTaskInventory inventory;
    public void setInventory(ZsDatasetTaskInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetTaskInventory getInventory() {
        return this.inventory;
    }

}
