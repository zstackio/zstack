package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class DownloadBitsFromRemoteTargetToPrimaryStorageReply extends MessageReply {
    private long diskSize;

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }
}
