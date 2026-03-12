package org.zstack.sdk;

import org.zstack.sdk.GpuDeviceInventory;

public class GpuResourceTreeNode  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String nodeType;
    public void setNodeType(java.lang.String nodeType) {
        this.nodeType = nodeType;
    }
    public java.lang.String getNodeType() {
        return this.nodeType;
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

    public GpuDeviceInventory gpu;
    public void setGpu(GpuDeviceInventory gpu) {
        this.gpu = gpu;
    }
    public GpuDeviceInventory getGpu() {
        return this.gpu;
    }

    public int mdevChildrenCount;
    public void setMdevChildrenCount(int mdevChildrenCount) {
        this.mdevChildrenCount = mdevChildrenCount;
    }
    public int getMdevChildrenCount() {
        return this.mdevChildrenCount;
    }

    public java.util.List children;
    public void setChildren(java.util.List children) {
        this.children = children;
    }
    public java.util.List getChildren() {
        return this.children;
    }

}
