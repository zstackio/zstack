package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 */
public class CreateTemplateFromVolumeSnapshotMsg extends NeedReplyMessage implements VolumeSnapshotMessage {
    private String imageUuid;
    private String snapshotUuid;
    private List<String> backupStorageUuids;
    private String volumeUuid;
    /**
     * @ignore
     */
    private String treeUuid;

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
}
