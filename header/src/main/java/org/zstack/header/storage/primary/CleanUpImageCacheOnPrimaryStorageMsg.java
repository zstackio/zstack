package org.zstack.header.storage.primary;


import org.zstack.header.message.NeedReplyMessage;

public class CleanUpImageCacheOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String uuid;
    private boolean force;

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
