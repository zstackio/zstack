package org.zstack.sdk;

import org.zstack.sdk.ZBoxInventory;

public class AddZBoxResult {
    public ZBoxInventory inventory;
    public void setInventory(ZBoxInventory inventory) {
        this.inventory = inventory;
    }
    public ZBoxInventory getInventory() {
        return this.inventory;
    }

}
