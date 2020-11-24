package org.zstack.sdk;

import org.zstack.sdk.BareMetal2ProvisionNetworkInventory;

public class AttachBareMetal2ProvisionNetworkToClusterResult {
    public BareMetal2ProvisionNetworkInventory inventory;
    public void setInventory(BareMetal2ProvisionNetworkInventory inventory) {
        this.inventory = inventory;
    }
    public BareMetal2ProvisionNetworkInventory getInventory() {
        return this.inventory;
    }

}
