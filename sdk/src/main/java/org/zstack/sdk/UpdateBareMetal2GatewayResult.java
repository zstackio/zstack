package org.zstack.sdk;

import org.zstack.sdk.BareMetal2GatewayInventory;

public class UpdateBareMetal2GatewayResult {
    public BareMetal2GatewayInventory inventory;
    public void setInventory(BareMetal2GatewayInventory inventory) {
        this.inventory = inventory;
    }
    public BareMetal2GatewayInventory getInventory() {
        return this.inventory;
    }

}
