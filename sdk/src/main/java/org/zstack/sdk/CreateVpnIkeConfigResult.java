package org.zstack.sdk;

import org.zstack.sdk.VpcVpnIkeConfigInventory;

public class CreateVpnIkeConfigResult {
    public VpcVpnIkeConfigInventory inventory;
    public void setInventory(VpcVpnIkeConfigInventory inventory) {
        this.inventory = inventory;
    }
    public VpcVpnIkeConfigInventory getInventory() {
        return this.inventory;
    }

}
