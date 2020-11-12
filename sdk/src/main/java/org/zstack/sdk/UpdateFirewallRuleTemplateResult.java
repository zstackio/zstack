package org.zstack.sdk;

import org.zstack.sdk.VpcFirewallRuleTemplateInventory;

public class UpdateFirewallRuleTemplateResult {
    public VpcFirewallRuleTemplateInventory inventory;
    public void setInventory(VpcFirewallRuleTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public VpcFirewallRuleTemplateInventory getInventory() {
        return this.inventory;
    }

}
