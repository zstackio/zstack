package org.zstack.sdk;

import org.zstack.sdk.L2VlanNetworkInventory;

public class CreateL2VlanNetworkResult {
    public L2VlanNetworkInventory inventory;
    public void setInventory(L2VlanNetworkInventory inventory) {
        this.inventory = inventory;
    }
    public L2VlanNetworkInventory getInventory() {
        return this.inventory;
    }

}
