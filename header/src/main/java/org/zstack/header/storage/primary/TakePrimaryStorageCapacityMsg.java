package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 6/18/2015.
 */
public class TakePrimaryStorageCapacityMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private long size;

    @Override
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
