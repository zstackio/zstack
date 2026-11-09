package org.zstack.header.allocator;

public class RequiredDiskCapacity {
    private String primaryStorageUuid;
    private long size;

    public RequiredDiskCapacity() {
    }

    public RequiredDiskCapacity(String primaryStorageUuid, long size) {
        this.primaryStorageUuid = primaryStorageUuid;
        this.size = size;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
