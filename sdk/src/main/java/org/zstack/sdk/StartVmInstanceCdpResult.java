package org.zstack.sdk;

import org.zstack.sdk.CdpTaskInventory;

public class StartVmInstanceCdpResult {
    public CdpTaskInventory inventory;
    public void setInventory(CdpTaskInventory inventory) {
        this.inventory = inventory;
    }
    public CdpTaskInventory getInventory() {
        return this.inventory;
    }

}
