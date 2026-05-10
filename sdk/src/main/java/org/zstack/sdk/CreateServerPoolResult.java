package org.zstack.sdk;

import org.zstack.sdk.ServerPoolInventory;

public class CreateServerPoolResult {
    public ServerPoolInventory inventory;
    public void setInventory(ServerPoolInventory inventory) {
        this.inventory = inventory;
    }
    public ServerPoolInventory getInventory() {
        return this.inventory;
    }

}
