package org.zstack.sdk;

import org.zstack.sdk.LongJobInventory;

public class ConvertVmFromForeignHypervisorResult {
    public LongJobInventory inventory;
    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }
    public LongJobInventory getInventory() {
        return this.inventory;
    }

}
