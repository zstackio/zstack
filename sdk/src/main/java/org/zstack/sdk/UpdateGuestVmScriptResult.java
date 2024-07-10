package org.zstack.sdk;

import org.zstack.sdk.GuestVmScriptInventory;

public class UpdateGuestVmScriptResult {
    public GuestVmScriptInventory inventory;
    public void setInventory(GuestVmScriptInventory inventory) {
        this.inventory = inventory;
    }
    public GuestVmScriptInventory getInventory() {
        return this.inventory;
    }

}
