package org.zstack.sdk;

import org.zstack.sdk.ApplianceVmInventory;

public class ProvisionVirtualRouterConfigResult {
    public ApplianceVmInventory inventory;
    public void setInventory(ApplianceVmInventory inventory) {
        this.inventory = inventory;
    }
    public ApplianceVmInventory getInventory() {
        return this.inventory;
    }

}
