package org.zstack.sdk;

import org.zstack.sdk.VmInstanceInventory;

public class FlattenVmInstanceResult {
    public VmInstanceInventory inventory;
    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public VmInstanceInventory getInventory() {
        return this.inventory;
    }

}
