package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

public class CheckJobOnPrimaryStorageMsg extends NeedReplyMessage {
    private String checkApiId;
    private String primaryStorageUuid;

    public String getCheckApiId() {
        return checkApiId;
    }

    public void setCheckApiId(String checkApiId) {
        this.checkApiId = checkApiId;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
