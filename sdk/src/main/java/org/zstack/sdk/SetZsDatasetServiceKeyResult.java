package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetServiceKeyInventory;

public class SetZsDatasetServiceKeyResult {
    public ZsDatasetServiceKeyInventory inventory;
    public void setInventory(ZsDatasetServiceKeyInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetServiceKeyInventory getInventory() {
        return this.inventory;
    }

}
