package org.zstack.sdk;

import org.zstack.sdk.VpcSharedQosInventory;

public class CreateVpcSharedQosResult {
    public VpcSharedQosInventory inventory;
    public void setInventory(VpcSharedQosInventory inventory) {
        this.inventory = inventory;
    }
    public VpcSharedQosInventory getInventory() {
        return this.inventory;
    }

}
