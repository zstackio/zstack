package org.zstack.sdk;

import org.zstack.sdk.BareMetal2InstanceInventory;

public class DetachProvisionNicFromBondingResult {
    public BareMetal2InstanceInventory inventory;
    public void setInventory(BareMetal2InstanceInventory inventory) {
        this.inventory = inventory;
    }
    public BareMetal2InstanceInventory getInventory() {
        return this.inventory;
    }

}
