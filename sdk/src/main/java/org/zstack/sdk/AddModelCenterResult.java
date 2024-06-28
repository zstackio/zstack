package org.zstack.sdk;

import org.zstack.sdk.ModelCenterInventory;

public class AddModelCenterResult {
    public ModelCenterInventory inventory;
    public void setInventory(ModelCenterInventory inventory) {
        this.inventory = inventory;
    }
    public ModelCenterInventory getInventory() {
        return this.inventory;
    }

}
