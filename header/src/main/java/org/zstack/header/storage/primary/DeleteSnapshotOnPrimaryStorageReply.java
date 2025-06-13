package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 */
public class DeleteSnapshotOnPrimaryStorageReply extends MessageReply {
    private boolean gcSubmitted = false;

    public void setGcSubmitted(boolean gcSubmitted) {
        this.gcSubmitted = gcSubmitted;
    }

    public boolean isGcSubmitted() {
        return gcSubmitted;
    }
}
