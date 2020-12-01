package org.zstack.storage.primary;

import org.zstack.header.message.MessageReply;

public class CheckPrimaryStorageCapacityReply extends MessageReply {
    private boolean isCapacity = false;

    public boolean isCapacity() {
        return isCapacity;
    }

    public void setCapacity(boolean capacity) {
        isCapacity = capacity;
    }
}
