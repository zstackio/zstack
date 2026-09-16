package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetPublicationInventory;

public class PublishZsDatasetSpaceResult {
    public ZsDatasetPublicationInventory inventory;
    public void setInventory(ZsDatasetPublicationInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetPublicationInventory getInventory() {
        return this.inventory;
    }

}
