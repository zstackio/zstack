package org.zstack.sdk;



public class DGpuProfileInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String gpuSpecUuid;
    public void setGpuSpecUuid(java.lang.String gpuSpecUuid) {
        this.gpuSpecUuid = gpuSpecUuid;
    }
    public java.lang.String getGpuSpecUuid() {
        return this.gpuSpecUuid;
    }

    public java.lang.Long memorySize;
    public void setMemorySize(java.lang.Long memorySize) {
        this.memorySize = memorySize;
    }
    public java.lang.Long getMemorySize() {
        return this.memorySize;
    }

    public java.lang.Long shmemSize;
    public void setShmemSize(java.lang.Long shmemSize) {
        this.shmemSize = shmemSize;
    }
    public java.lang.Long getShmemSize() {
        return this.shmemSize;
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

}
