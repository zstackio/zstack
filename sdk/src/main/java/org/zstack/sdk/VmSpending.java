package org.zstack.sdk;

public class VmSpending extends SpendingDetails {

    public java.util.List<VmSpendingDetails> cpuInventory;
    public void setCpuInventory(java.util.List<VmSpendingDetails> cpuInventory) {
        this.cpuInventory = cpuInventory;
    }
    public java.util.List<VmSpendingDetails> getCpuInventory() {
        return this.cpuInventory;
    }

    public java.util.List<VmSpendingDetails> memoryInventory;
    public void setMemoryInventory(java.util.List<VmSpendingDetails> memoryInventory) {
        this.memoryInventory = memoryInventory;
    }
    public java.util.List<VmSpendingDetails> getMemoryInventory() {
        return this.memoryInventory;
    }

    public java.util.List<VmSpendingDetails> rootVolumeInventory;
    public void setRootVolumeInventory(java.util.List<VmSpendingDetails> rootVolumeInventory) {
        this.rootVolumeInventory = rootVolumeInventory;
    }
    public java.util.List<VmSpendingDetails> getRootVolumeInventory() {
        return this.rootVolumeInventory;
    }

}
