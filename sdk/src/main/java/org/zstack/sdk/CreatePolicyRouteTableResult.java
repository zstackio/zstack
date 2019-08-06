package org.zstack.sdk;

import org.zstack.sdk.PolicyRouteTableInventory;

public class CreatePolicyRouteTableResult {
    public PolicyRouteTableInventory inventory;
    public void setInventory(PolicyRouteTableInventory inventory) {
        this.inventory = inventory;
    }
    public PolicyRouteTableInventory getInventory() {
        return this.inventory;
    }

}
