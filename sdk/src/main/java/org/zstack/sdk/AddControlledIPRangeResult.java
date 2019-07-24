package org.zstack.sdk;

import org.zstack.sdk.ControlledIpRangeInventory;

public class AddControlledIPRangeResult {
    public ControlledIpRangeInventory inventory;
    public void setInventory(ControlledIpRangeInventory inventory) {
        this.inventory = inventory;
    }
    public ControlledIpRangeInventory getInventory() {
        return this.inventory;
    }

}
