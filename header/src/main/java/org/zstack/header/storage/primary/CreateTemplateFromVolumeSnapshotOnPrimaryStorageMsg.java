package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

import java.util.List;

/**
 */
public class CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    public static class SnapshotDownloadInfo {
        private String backupStorageUuid;
        private String backupStorageInstallPath;
        private VolumeSnapshotInventory snapshot;

        public VolumeSnapshotInventory getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(VolumeSnapshotInventory snapshot) {
            this.snapshot = snapshot;
        }

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public String getBackupStorageInstallPath() {
            return backupStorageInstallPath;
        }

        public void setBackupStorageInstallPath(String backupStorageInstallPath) {
            this.backupStorageInstallPath = backupStorageInstallPath;
        }
    }

    private String imageUuid;
    private String primaryStorageUuid;
    private List<SnapshotDownloadInfo> snapshotsDownloadInfo;
    private List<BackupStorageInventory> backupStorage;
    private boolean needDownload;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<BackupStorageInventory> getBackupStorage() {
        return backupStorage;
    }

    public void setBackupStorage(List<BackupStorageInventory> backupStorage) {
        this.backupStorage = backupStorage;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<SnapshotDownloadInfo> getSnapshotsDownloadInfo() {
        return snapshotsDownloadInfo;
    }

    public void setSnapshotsDownloadInfo(List<SnapshotDownloadInfo> snapshotsDownloadInfo) {
        this.snapshotsDownloadInfo = snapshotsDownloadInfo;
    }

    public boolean isNeedDownload() {
        return needDownload;
    }

    public void setNeedDownload(boolean needDownload) {
        this.needDownload = needDownload;
    }
}
