package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetSpaceInventory;

public class EditZsDatasetSpaceResult {
    public ZsDatasetSpaceInventory inventory;
    public void setInventory(ZsDatasetSpaceInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetSpaceInventory getInventory() {
        return this.inventory;
    }

}
