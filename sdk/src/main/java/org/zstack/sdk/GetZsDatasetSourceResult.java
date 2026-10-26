package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetSourceInventory;

public class GetZsDatasetSourceResult {
    public ZsDatasetSourceInventory inventory;
    public void setInventory(ZsDatasetSourceInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetSourceInventory getInventory() {
        return this.inventory;
    }

}
