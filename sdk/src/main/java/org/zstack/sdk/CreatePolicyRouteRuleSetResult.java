package org.zstack.sdk;

import org.zstack.sdk.PolicyRouteRuleSetInventory;

public class CreatePolicyRouteRuleSetResult {
    public PolicyRouteRuleSetInventory inventory;
    public void setInventory(PolicyRouteRuleSetInventory inventory) {
        this.inventory = inventory;
    }
    public PolicyRouteRuleSetInventory getInventory() {
        return this.inventory;
    }

}
