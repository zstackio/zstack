package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetSegmentationSettingsInventory;

public class GetZsDatasetSegmentationSettingsResult {
    public ZsDatasetSegmentationSettingsInventory inventory;
    public void setInventory(ZsDatasetSegmentationSettingsInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetSegmentationSettingsInventory getInventory() {
        return this.inventory;
    }

}
