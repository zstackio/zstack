package org.zstack.sdk;

import org.zstack.sdk.L2VxlanNetworkInventory;

public class CreateL2HardwareVxlanNetworkResult {
    public L2VxlanNetworkInventory inventory;
    public void setInventory(L2VxlanNetworkInventory inventory) {
        this.inventory = inventory;
    }
    public L2VxlanNetworkInventory getInventory() {
        return this.inventory;
    }

}
