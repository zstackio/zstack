package org.zstack.sdk;

import org.zstack.sdk.DatasetInventory;

public class UpdateDatasetResult {
    public DatasetInventory inventory;
    public void setInventory(DatasetInventory inventory) {
        this.inventory = inventory;
    }
    public DatasetInventory getInventory() {
        return this.inventory;
    }

}
