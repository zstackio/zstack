package org.zstack.sdk;

import org.zstack.sdk.MemorySwapDiskDeviceInventory;

public class MemorySwapDiskInventory  {

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public MemorySwapDiskDeviceInventory swapDisk;
    public void setSwapDisk(MemorySwapDiskDeviceInventory swapDisk) {
        this.swapDisk = swapDisk;
    }
    public MemorySwapDiskDeviceInventory getSwapDisk() {
        return this.swapDisk;
    }

    public java.util.List otherSwaps;
    public void setOtherSwaps(java.util.List otherSwaps) {
        this.otherSwaps = otherSwaps;
    }
    public java.util.List getOtherSwaps() {
        return this.otherSwaps;
    }

    public java.util.List preparationReasons;
    public void setPreparationReasons(java.util.List preparationReasons) {
        this.preparationReasons = preparationReasons;
    }
    public java.util.List getPreparationReasons() {
        return this.preparationReasons;
    }

    public java.util.List candidates;
    public void setCandidates(java.util.List candidates) {
        this.candidates = candidates;
    }
    public java.util.List getCandidates() {
        return this.candidates;
    }

}
