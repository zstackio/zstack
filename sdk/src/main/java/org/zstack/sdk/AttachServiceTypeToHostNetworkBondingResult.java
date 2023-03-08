package org.zstack.sdk;

import org.zstack.sdk.HostNetworkBondingServiceRefInventory;

public class AttachServiceTypeToHostNetworkBondingResult {
    public HostNetworkBondingServiceRefInventory inventory;
    public void setInventory(HostNetworkBondingServiceRefInventory inventory) {
        this.inventory = inventory;
    }
    public HostNetworkBondingServiceRefInventory getInventory() {
        return this.inventory;
    }

}
