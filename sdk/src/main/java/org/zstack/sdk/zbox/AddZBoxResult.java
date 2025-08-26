package org.zstack.sdk.zbox;

import org.zstack.sdk.zbox.ZBoxInventory;

public class AddZBoxResult {
    public ZBoxInventory inventory;
    public void setInventory(ZBoxInventory inventory) {
        this.inventory = inventory;
    }
    public ZBoxInventory getInventory() {
        return this.inventory;
    }

}
