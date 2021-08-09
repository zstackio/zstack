package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class AllocatePrimaryStorageReply extends MessageReply {
    private PrimaryStorageInventory primaryStorageInventory;
    private long size;

    // add installDir
    private String installDir;

    public String getInstallDir() {
        return installDir;
    }

    public void setInstallDir(String installDir) {
        this.installDir = installDir;
    }

    public AllocatePrimaryStorageReply(PrimaryStorageInventory primaryStorageInventory) {
        super();
        this.primaryStorageInventory = primaryStorageInventory;
    }

    public PrimaryStorageInventory getPrimaryStorageInventory() {
        return primaryStorageInventory;
    }

    public void setPrimaryStorageInventory(PrimaryStorageInventory primaryStorageInventory) {
        this.primaryStorageInventory = primaryStorageInventory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
