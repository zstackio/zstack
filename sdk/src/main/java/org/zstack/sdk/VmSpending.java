package org.zstack.sdk;



public class VmSpending extends org.zstack.sdk.SpendingDetails {

    public java.lang.String hypervisorType;
    public void setHypervisorType(java.lang.String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
    public java.lang.String getHypervisorType() {
        return this.hypervisorType;
    }

    public java.util.List cpuInventory;
    public void setCpuInventory(java.util.List cpuInventory) {
        this.cpuInventory = cpuInventory;
    }
    public java.util.List getCpuInventory() {
        return this.cpuInventory;
    }

    public java.util.List memoryInventory;
    public void setMemoryInventory(java.util.List memoryInventory) {
        this.memoryInventory = memoryInventory;
    }
    public java.util.List getMemoryInventory() {
        return this.memoryInventory;
    }

    public java.util.List rootVolumeInventory;
    public void setRootVolumeInventory(java.util.List rootVolumeInventory) {
        this.rootVolumeInventory = rootVolumeInventory;
    }
    public java.util.List getRootVolumeInventory() {
        return this.rootVolumeInventory;
    }

}
