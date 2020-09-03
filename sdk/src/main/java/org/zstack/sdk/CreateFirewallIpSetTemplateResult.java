package org.zstack.sdk;

import org.zstack.sdk.VpcFirewallIpSetTemplateInventory;

public class CreateFirewallIpSetTemplateResult {
    public VpcFirewallIpSetTemplateInventory inventory;
    public void setInventory(VpcFirewallIpSetTemplateInventory inventory) {
        this.inventory = inventory;
    }
    public VpcFirewallIpSetTemplateInventory getInventory() {
        return this.inventory;
    }

}
