package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetTransferCommitInventory;

public class CommitZsDatasetTransferResult {
    public ZsDatasetTransferCommitInventory inventory;
    public void setInventory(ZsDatasetTransferCommitInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetTransferCommitInventory getInventory() {
        return this.inventory;
    }

}
