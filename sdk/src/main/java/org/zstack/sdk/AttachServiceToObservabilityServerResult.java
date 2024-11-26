package org.zstack.sdk;

import org.zstack.sdk.ObservabilityServerVmInventory;

public class AttachServiceToObservabilityServerResult {
    public ObservabilityServerVmInventory inventory;
    public void setInventory(ObservabilityServerVmInventory inventory) {
        this.inventory = inventory;
    }
    public ObservabilityServerVmInventory getInventory() {
        return this.inventory;
    }

}
