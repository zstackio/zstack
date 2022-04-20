package org.zstack.sdk;

import org.zstack.sdk.L2GatewayInventory;

public class CreateL2GatewayResult {
    public L2GatewayInventory inventory;
    public void setInventory(L2GatewayInventory inventory) {
        this.inventory = inventory;
    }
    public L2GatewayInventory getInventory() {
        return this.inventory;
    }

}
