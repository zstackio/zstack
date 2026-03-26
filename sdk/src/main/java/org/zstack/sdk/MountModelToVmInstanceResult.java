package org.zstack.sdk;

import org.zstack.sdk.VmModelMountInventory;

public class MountModelToVmInstanceResult {
    public VmModelMountInventory inventory;
    public void setInventory(VmModelMountInventory inventory) {
        this.inventory = inventory;
    }
    public VmModelMountInventory getInventory() {
        return this.inventory;
    }

}
