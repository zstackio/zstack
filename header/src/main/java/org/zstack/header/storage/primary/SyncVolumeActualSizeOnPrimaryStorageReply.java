package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/4/24.
 */
public class SyncVolumeActualSizeOnPrimaryStorageReply extends MessageReply {
    private long actualSize;

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }
}
