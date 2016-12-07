package org.zstack.sdk;

public class ChangeL3NetworkStateResult {
    public L3NetworkInventory inventory;
    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
    public L3NetworkInventory getInventory() {
        return this.inventory;
    }

}
