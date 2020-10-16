package org.zstack.sdk;

import org.zstack.sdk.SlbVmInstanceInventory;

public class CreateSlbInstanceResult {
    public SlbVmInstanceInventory inventory;
    public void setInventory(SlbVmInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public SlbVmInstanceInventory getInventory() {
        return this.inventory;
    }

}
