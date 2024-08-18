package org.zstack.sdk;

import org.zstack.sdk.ContainerManagementVmInventory;

public class AddContainerManagementVmResult {
    public ContainerManagementVmInventory inventory;
    public void setInventory(ContainerManagementVmInventory inventory) {
        this.inventory = inventory;
    }
    public ContainerManagementVmInventory getInventory() {
        return this.inventory;
    }

}
