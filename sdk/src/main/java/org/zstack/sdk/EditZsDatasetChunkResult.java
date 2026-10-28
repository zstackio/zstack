package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetChunkInventory;

public class EditZsDatasetChunkResult {
    public ZsDatasetChunkInventory inventory;
    public void setInventory(ZsDatasetChunkInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetChunkInventory getInventory() {
        return this.inventory;
    }

}
