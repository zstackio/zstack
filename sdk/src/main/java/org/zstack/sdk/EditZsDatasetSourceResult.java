package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetSourceInventory;

public class EditZsDatasetSourceResult {
    public ZsDatasetSourceInventory inventory;
    public void setInventory(ZsDatasetSourceInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetSourceInventory getInventory() {
        return this.inventory;
    }

}
