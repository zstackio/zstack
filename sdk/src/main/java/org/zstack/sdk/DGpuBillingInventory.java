package org.zstack.sdk;



public class DGpuBillingInventory extends org.zstack.sdk.BillingInventory {

    public java.lang.String vmName;
    public void setVmName(java.lang.String vmName) {
        this.vmName = vmName;
    }
    public java.lang.String getVmName() {
        return this.vmName;
    }

    public java.lang.Long allocatedMemory;
    public void setAllocatedMemory(java.lang.Long allocatedMemory) {
        this.allocatedMemory = allocatedMemory;
    }
    public java.lang.Long getAllocatedMemory() {
        return this.allocatedMemory;
    }

}
