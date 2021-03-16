package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2021/3/18.
 */
public class CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply extends MessageReply {
    private String locateHostUuid;
    private long actualSize;

    public String getLocateHostUuid() {
        return locateHostUuid;
    }

    public void setLocateHostUuid(String locateHostUuid) {
        this.locateHostUuid = locateHostUuid;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }
}
