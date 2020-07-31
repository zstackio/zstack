package org.zstack.sdk;

import org.zstack.sdk.VmInstanceInventory;
import org.zstack.sdk.VmInstanceInventory;

public class GetFaultToleranceVmsResult {
    public VmInstanceInventory primaryVmInventory;
    public void setPrimaryVmInventory(VmInstanceInventory primaryVmInventory) {
        this.primaryVmInventory = primaryVmInventory;
    }
    public VmInstanceInventory getPrimaryVmInventory() {
        return this.primaryVmInventory;
    }

    public VmInstanceInventory secondaryVmInventory;
    public void setSecondaryVmInventory(VmInstanceInventory secondaryVmInventory) {
        this.secondaryVmInventory = secondaryVmInventory;
    }
    public VmInstanceInventory getSecondaryVmInventory() {
        return this.secondaryVmInventory;
    }

}
