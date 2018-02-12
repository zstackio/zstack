package org.zstack.sdk;

import org.zstack.sdk.VpcVpnConnectionInventory;

public class UpdateVpcVpnConnectionRemoteResult {
    public VpcVpnConnectionInventory inventory;
    public void setInventory(VpcVpnConnectionInventory inventory) {
        this.inventory = inventory;
    }
    public VpcVpnConnectionInventory getInventory() {
        return this.inventory;
    }

}
