package org.zstack.sdk;

import org.zstack.sdk.ModelInventory;

public class AddModelResult {
    public ModelInventory inventory;
    public void setInventory(ModelInventory inventory) {
        this.inventory = inventory;
    }
    public ModelInventory getInventory() {
        return this.inventory;
    }

}
