package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetTransferInventory;

public class CancelZsDatasetTransferResult {
    public ZsDatasetTransferInventory inventory;
    public void setInventory(ZsDatasetTransferInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetTransferInventory getInventory() {
        return this.inventory;
    }

}
