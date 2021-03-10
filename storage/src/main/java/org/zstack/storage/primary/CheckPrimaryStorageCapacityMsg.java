package org.zstack.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

public class CheckPrimaryStorageCapacityMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;

    private Long requiredSize;

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public Long getRequiredSize() {
        return requiredSize;
    }

    public void setRequiredSize(Long requiredSize) {
        this.requiredSize = requiredSize;
    }
}
