package org.zstack.sdk;

import org.zstack.sdk.HostNetworkLabelInventory;

public class CreateHostNetworkServiceTypeResult {
    public HostNetworkLabelInventory inventory;
    public void setInventory(HostNetworkLabelInventory inventory) {
        this.inventory = inventory;
    }
    public HostNetworkLabelInventory getInventory() {
        return this.inventory;
    }

}
