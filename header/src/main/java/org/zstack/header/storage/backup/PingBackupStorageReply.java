package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 */
public class PingBackupStorageReply extends MessageReply {
    private boolean available;
    private boolean connected;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
