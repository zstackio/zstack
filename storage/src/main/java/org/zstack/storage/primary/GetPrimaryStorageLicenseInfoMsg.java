package org.zstack.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

public class GetPrimaryStorageLicenseInfoMsg extends NeedReplyMessage {
    private String primaryStorageUuid;

    private boolean getCache = false;

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public boolean isGetCache() {
        return getCache;
    }

    public void setGetCache(boolean getCache) {
        this.getCache = getCache;
    }
}
