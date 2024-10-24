package org.zstack.sdk;

import org.zstack.sdk.HostNetworkLabelInventory;

public class UpdateHostNetworkServiceTypeResult {
    public HostNetworkLabelInventory inventory;
    public void setInventory(HostNetworkLabelInventory inventory) {
        this.inventory = inventory;
    }
    public HostNetworkLabelInventory getInventory() {
        return this.inventory;
    }

}
