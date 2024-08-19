package org.zstack.sdk;

import org.zstack.sdk.GuestVmScriptExecutedRecordInventory;

public class ExecuteGuestVmScriptResult {
    public GuestVmScriptExecutedRecordInventory inventory;
    public void setInventory(GuestVmScriptExecutedRecordInventory inventory) {
        this.inventory = inventory;
    }
    public GuestVmScriptExecutedRecordInventory getInventory() {
        return this.inventory;
    }

}
