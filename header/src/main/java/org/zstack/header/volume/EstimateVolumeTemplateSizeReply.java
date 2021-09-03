package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

public class EstimateVolumeTemplateSizeReply extends MessageReply {
    private long actualSize;
    private long size;

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
