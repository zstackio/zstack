package org.zstack.sdk;

import org.zstack.sdk.ExternalManagementNodeInventory;

public class AddExternalManagementNodeResult {
    public ExternalManagementNodeInventory inventory;
    public void setInventory(ExternalManagementNodeInventory inventory) {
        this.inventory = inventory;
    }
    public ExternalManagementNodeInventory getInventory() {
        return this.inventory;
    }

}
