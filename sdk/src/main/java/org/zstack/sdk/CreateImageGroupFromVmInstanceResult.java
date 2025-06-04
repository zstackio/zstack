package org.zstack.sdk;

import org.zstack.sdk.ImageGroupInventory;

public class CreateImageGroupFromVmInstanceResult {
    public ImageGroupInventory inventory;
    public void setInventory(ImageGroupInventory inventory) {
        this.inventory = inventory;
    }
    public ImageGroupInventory getInventory() {
        return this.inventory;
    }

}
