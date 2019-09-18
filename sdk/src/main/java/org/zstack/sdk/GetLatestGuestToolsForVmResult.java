package org.zstack.sdk;

import org.zstack.sdk.GuestToolsInventory;

public class GetLatestGuestToolsForVmResult {
    public GuestToolsInventory inventory;
    public void setInventory(GuestToolsInventory inventory) {
        this.inventory = inventory;
    }
    public GuestToolsInventory getInventory() {
        return this.inventory;
    }

}
