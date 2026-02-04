package org.zstack.sdk;

import org.zstack.sdk.HaNetworkGroupInventory;

public class UpdateHaNetworkGroupResult {
    public HaNetworkGroupInventory inventory;
    public void setInventory(HaNetworkGroupInventory inventory) {
        this.inventory = inventory;
    }
    public HaNetworkGroupInventory getInventory() {
        return this.inventory;
    }

}
