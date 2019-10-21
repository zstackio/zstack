package org.zstack.sdk;

import org.zstack.sdk.SdnControllerInventory;

public class UpdateSdnControllerResult {
    public SdnControllerInventory inventory;
    public void setInventory(SdnControllerInventory inventory) {
        this.inventory = inventory;
    }
    public SdnControllerInventory getInventory() {
        return this.inventory;
    }

}
