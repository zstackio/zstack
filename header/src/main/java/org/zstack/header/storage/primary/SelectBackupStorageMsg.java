package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2019/10/10.
 */
public class SelectBackupStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String volumeUuid;
    private long requiredSize;

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public long getRequiredSize() {
        return requiredSize;
    }

    public void setRequiredSize(long requiredSize) {
        this.requiredSize = requiredSize;
    }
}
