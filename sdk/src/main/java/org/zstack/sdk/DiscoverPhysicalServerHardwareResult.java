package org.zstack.sdk;

import org.zstack.sdk.PhysicalServerInventory;

public class DiscoverPhysicalServerHardwareResult {
    public PhysicalServerInventory inventory;
    public void setInventory(PhysicalServerInventory inventory) {
        this.inventory = inventory;
    }
    public PhysicalServerInventory getInventory() {
        return this.inventory;
    }

}
