package org.zstack.sdk;

import org.zstack.sdk.DGpuStatus;

public class DGpuDeviceInventory  {

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

    public java.lang.String parentGpuUuid;
    public void setParentGpuUuid(java.lang.String parentGpuUuid) {
        this.parentGpuUuid = parentGpuUuid;
    }
    public java.lang.String getParentGpuUuid() {
        return this.parentGpuUuid;
    }

    public java.lang.String gpuSpecUuid;
    public void setGpuSpecUuid(java.lang.String gpuSpecUuid) {
        this.gpuSpecUuid = gpuSpecUuid;
    }
    public java.lang.String getGpuSpecUuid() {
        return this.gpuSpecUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.Long allocatedMemory;
    public void setAllocatedMemory(java.lang.Long allocatedMemory) {
        this.allocatedMemory = allocatedMemory;
    }
    public java.lang.Long getAllocatedMemory() {
        return this.allocatedMemory;
    }

    public java.lang.Long shmemSize;
    public void setShmemSize(java.lang.Long shmemSize) {
        this.shmemSize = shmemSize;
    }
    public java.lang.Long getShmemSize() {
        return this.shmemSize;
    }

    public java.lang.String protocol;
    public void setProtocol(java.lang.String protocol) {
        this.protocol = protocol;
    }
    public java.lang.String getProtocol() {
        return this.protocol;
    }

    public java.lang.Integer smPercentLimit;
    public void setSmPercentLimit(java.lang.Integer smPercentLimit) {
        this.smPercentLimit = smPercentLimit;
    }
    public java.lang.Integer getSmPercentLimit() {
        return this.smPercentLimit;
    }

    public DGpuStatus status;
    public void setStatus(DGpuStatus status) {
        this.status = status;
    }
    public DGpuStatus getStatus() {
        return this.status;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.lang.String vendorId;
    public void setVendorId(java.lang.String vendorId) {
        this.vendorId = vendorId;
    }
    public java.lang.String getVendorId() {
        return this.vendorId;
    }

    public java.lang.String vendor;
    public void setVendor(java.lang.String vendor) {
        this.vendor = vendor;
    }
    public java.lang.String getVendor() {
        return this.vendor;
    }

}
