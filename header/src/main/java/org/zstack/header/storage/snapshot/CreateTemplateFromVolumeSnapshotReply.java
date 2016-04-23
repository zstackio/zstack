package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

import java.util.Map;

/**
 */
public class CreateTemplateFromVolumeSnapshotReply extends MessageReply {
    private long actualSize;
    private long size;
    // key = backup storage uuid, value = install path
    private Map<String, String> onBackupStorage;

    public Map<String, String> getOnBackupStorage() {
        return onBackupStorage;
    }

    public void setOnBackupStorage(Map<String, String> onBackupStorage) {
        this.onBackupStorage = onBackupStorage;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
