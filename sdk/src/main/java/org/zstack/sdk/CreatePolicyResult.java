package org.zstack.sdk;

import org.zstack.sdk.PolicyInventory;

public class CreatePolicyResult {
    public PolicyInventory inventory;
    public void setInventory(PolicyInventory inventory) {
        this.inventory = inventory;
    }
    public PolicyInventory getInventory() {
        return this.inventory;
    }

}
