package org.zstack.sdk;

import org.zstack.sdk.GuestToolsStateInventory;

public class UpdateGuestToolsStateResult {
    public GuestToolsStateInventory inventory;
    public void setInventory(GuestToolsStateInventory inventory) {
        this.inventory = inventory;
    }
    public GuestToolsStateInventory getInventory() {
        return this.inventory;
    }

}
