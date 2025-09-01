package org.zstack.sdk;

import org.zstack.sdk.ImageGroupInventory;

public class CreateImageGroupFromSnapshotResult {
    public ImageGroupInventory inventory;
    public void setInventory(ImageGroupInventory inventory) {
        this.inventory = inventory;
    }
    public ImageGroupInventory getInventory() {
        return this.inventory;
    }

}
