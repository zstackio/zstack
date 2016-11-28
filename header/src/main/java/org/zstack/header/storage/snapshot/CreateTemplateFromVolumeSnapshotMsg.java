package org.zstack.header.storage.snapshot;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotMsg;
import org.zstack.header.message.NeedReplyMessage;

@ApiTimeout(apiClasses = {APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class})
public class CreateTemplateFromVolumeSnapshotMsg extends NeedReplyMessage implements VolumeSnapshotMessage {
    private String imageUuid;
    private String snapshotUuid;
    private String backupStorageUuid;
    private String volumeUuid;
    /**
     * @ignore
     */
    private String treeUuid;

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

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
}
