package org.zstack.storage.ceph.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by xing5 on 2016/5/4.
 */
public class GetVolumeSizeReply extends MessageReply {
    private long size;
    private long actualSize;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }
}
