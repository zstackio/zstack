package org.zstack.sdk;

import org.zstack.sdk.VpcFirewallRuleSetL3RefInventory;

public class AttachFirewallRuleSetToL3Result {
    public VpcFirewallRuleSetL3RefInventory inventory;
    public void setInventory(VpcFirewallRuleSetL3RefInventory inventory) {
        this.inventory = inventory;
    }
    public VpcFirewallRuleSetL3RefInventory getInventory() {
        return this.inventory;
    }

}
