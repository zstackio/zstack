package org.zstack.sdk;

import org.zstack.sdk.SecurityMachineInventory;

public class ChangeSecurityMachineStateResult {
    public SecurityMachineInventory inventory;
    public void setInventory(SecurityMachineInventory inventory) {
        this.inventory = inventory;
    }
    public SecurityMachineInventory getInventory() {
        return this.inventory;
    }

}
