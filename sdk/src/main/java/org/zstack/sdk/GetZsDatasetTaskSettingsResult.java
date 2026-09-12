package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetTaskSettingsInventory;

public class GetZsDatasetTaskSettingsResult {
    public ZsDatasetTaskSettingsInventory inventory;
    public void setInventory(ZsDatasetTaskSettingsInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetTaskSettingsInventory getInventory() {
        return this.inventory;
    }

}
