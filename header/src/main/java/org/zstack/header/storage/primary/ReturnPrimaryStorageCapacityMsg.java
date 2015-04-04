package org.zstack.header.storage.primary;

import org.zstack.header.message.Message;

public class ReturnPrimaryStorageCapacityMsg extends Message {
    private String primaryStorageUuid;
    private long diskSize;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }
}
