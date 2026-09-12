package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetImageInventory;

public class EditZsDatasetImageResult {
    public ZsDatasetImageInventory inventory;
    public void setInventory(ZsDatasetImageInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetImageInventory getInventory() {
        return this.inventory;
    }

}
