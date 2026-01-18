package org.zstack.sdk;

import org.zstack.sdk.ExternalServiceConfigurationInventory;

public class AddExternalServiceConfigurationResult {
    public ExternalServiceConfigurationInventory inventory;
    public void setInventory(ExternalServiceConfigurationInventory inventory) {
        this.inventory = inventory;
    }
    public ExternalServiceConfigurationInventory getInventory() {
        return this.inventory;
    }

}
