package org.zstack.sdk;

import org.zstack.sdk.SecurityLevelResourceRefInventory;

public class SetResourceSecurityLevelResult {
    public SecurityLevelResourceRefInventory inventory;
    public void setInventory(SecurityLevelResourceRefInventory inventory) {
        this.inventory = inventory;
    }
    public SecurityLevelResourceRefInventory getInventory() {
        return this.inventory;
    }

}
