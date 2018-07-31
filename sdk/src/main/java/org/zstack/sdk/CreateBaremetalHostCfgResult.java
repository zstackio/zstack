package org.zstack.sdk;

import org.zstack.sdk.BaremetalHostCfgInventory;

public class CreateBaremetalHostCfgResult {
    public BaremetalHostCfgInventory inventory;
    public void setInventory(BaremetalHostCfgInventory inventory) {
        this.inventory = inventory;
    }
    public BaremetalHostCfgInventory getInventory() {
        return this.inventory;
    }

}
