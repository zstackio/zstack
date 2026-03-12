package org.zstack.sdk;



public class GetGpuResourceTreeResult {
    public java.util.List tree;
    public void setTree(java.util.List tree) {
        this.tree = tree;
    }
    public java.util.List getTree() {
        return this.tree;
    }

    public int totalGpuCount;
    public void setTotalGpuCount(int totalGpuCount) {
        this.totalGpuCount = totalGpuCount;
    }
    public int getTotalGpuCount() {
        return this.totalGpuCount;
    }

    public int allocatedGpuCount;
    public void setAllocatedGpuCount(int allocatedGpuCount) {
        this.allocatedGpuCount = allocatedGpuCount;
    }
    public int getAllocatedGpuCount() {
        return this.allocatedGpuCount;
    }

    public int unallocatedGpuCount;
    public void setUnallocatedGpuCount(int unallocatedGpuCount) {
        this.unallocatedGpuCount = unallocatedGpuCount;
    }
    public int getUnallocatedGpuCount() {
        return this.unallocatedGpuCount;
    }

}
