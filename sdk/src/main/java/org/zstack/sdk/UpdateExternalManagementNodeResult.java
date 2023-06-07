package org.zstack.sdk;

import org.zstack.sdk.ExternalManagementNodeInventory;

public class UpdateExternalManagementNodeResult {
    public ExternalManagementNodeInventory inventory;
    public void setInventory(ExternalManagementNodeInventory inventory) {
        this.inventory = inventory;
    }
    public ExternalManagementNodeInventory getInventory() {
        return this.inventory;
    }

}
