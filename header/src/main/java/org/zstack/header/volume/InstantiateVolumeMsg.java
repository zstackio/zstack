package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/8/22.
 */
public class InstantiateVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;
    private String primaryStorageUuid;
    private boolean primaryStorageAllocated;
    private boolean skipIfExisting;

    public boolean isPrimaryStorageAllocated() {
        return primaryStorageAllocated;
    }

    public void setPrimaryStorageAllocated(boolean primaryStorageAllocated) {
        this.primaryStorageAllocated = primaryStorageAllocated;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public boolean isSkipIfExisting() {
        return skipIfExisting;
    }

    public void setSkipIfExisting(boolean skipIfExisting) {
        this.skipIfExisting = skipIfExisting;
    }
}
