package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;

import java.util.List;

/**
 */
public class CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String volumeUuid;
    private String primaryStorageUuid;
    private List<SnapshotDownloadInfo> snapshots;
    private boolean needDownload;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public boolean isNeedDownload() {
        return needDownload;
    }

    public void setNeedDownload(boolean needDownload) {
        this.needDownload = needDownload;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<SnapshotDownloadInfo> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<SnapshotDownloadInfo> snapshots) {
        this.snapshots = snapshots;
    }
}
