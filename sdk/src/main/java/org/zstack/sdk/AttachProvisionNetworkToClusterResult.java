package org.zstack.sdk;

import org.zstack.sdk.PhysicalServerProvisionNetworkInventory;

public class AttachProvisionNetworkToClusterResult {
    public PhysicalServerProvisionNetworkInventory inventory;
    public void setInventory(PhysicalServerProvisionNetworkInventory inventory) {
        this.inventory = inventory;
    }
    public PhysicalServerProvisionNetworkInventory getInventory() {
        return this.inventory;
    }

}
