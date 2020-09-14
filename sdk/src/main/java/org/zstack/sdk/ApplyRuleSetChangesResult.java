package org.zstack.sdk;

import org.zstack.sdk.VpcFirewallRuleSetInventory;

public class ApplyRuleSetChangesResult {
    public VpcFirewallRuleSetInventory inventory;
    public void setInventory(VpcFirewallRuleSetInventory inventory) {
        this.inventory = inventory;
    }
    public VpcFirewallRuleSetInventory getInventory() {
        return this.inventory;
    }

}
