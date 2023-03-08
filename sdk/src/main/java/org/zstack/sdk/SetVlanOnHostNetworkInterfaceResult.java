package org.zstack.sdk;

import org.zstack.sdk.HostNetworkInterfaceServiceRefInventory;

public class SetVlanOnHostNetworkInterfaceResult {
    public HostNetworkInterfaceServiceRefInventory inventory;
    public void setInventory(HostNetworkInterfaceServiceRefInventory inventory) {
        this.inventory = inventory;
    }
    public HostNetworkInterfaceServiceRefInventory getInventory() {
        return this.inventory;
    }

}
