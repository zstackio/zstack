package org.zstack.sdk;

public class CreateFirewallRuleSetResult {
    public VpcFirewallRuleSetInventory inventory;
    public void setInventory(VpcFirewallRuleSetInventory inventory) {
        this.inventory = inventory;
    }
    public VpcFirewallRuleSetInventory getInventory() {
        return this.inventory;
    }

}
