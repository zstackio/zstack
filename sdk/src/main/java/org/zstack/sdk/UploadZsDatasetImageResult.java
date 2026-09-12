package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetImageInventory;

public class UploadZsDatasetImageResult {
    public ZsDatasetImageInventory inventory;
    public void setInventory(ZsDatasetImageInventory inventory) {
        this.inventory = inventory;
    }
    public ZsDatasetImageInventory getInventory() {
        return this.inventory;
    }

    public boolean created;
    public void setCreated(boolean created) {
        this.created = created;
    }
    public boolean getCreated() {
        return this.created;
    }

}
