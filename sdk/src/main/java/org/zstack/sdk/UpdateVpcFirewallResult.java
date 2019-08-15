package org.zstack.sdk;

import org.zstack.sdk.VpcFirewallInventory;

public class UpdateVpcFirewallResult {
    public VpcFirewallInventory inventory;
    public void setInventory(VpcFirewallInventory inventory) {
        this.inventory = inventory;
    }
    public VpcFirewallInventory getInventory() {
        return this.inventory;
    }

}
