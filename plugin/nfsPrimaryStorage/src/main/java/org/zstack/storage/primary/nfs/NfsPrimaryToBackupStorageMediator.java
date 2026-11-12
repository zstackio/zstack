package org.zstack.storage.primary.nfs;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.ImageDownloadPurpose;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryToBackupStorageMediator;
import org.zstack.header.volume.VolumeInventory;

public interface NfsPrimaryToBackupStorageMediator extends PrimaryToBackupStorageMediator {
    void downloadBits(PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, boolean isData, Completion completion);

    default void downloadBits(String imageUuid, PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, ImageDownloadPurpose purpose, Completion completion) {
        downloadBits(pinv, bsinv, backupStorageInstallPath, primaryStorageInstallPath, purpose == ImageDownloadPurpose.DataVolume, completion);
    }

    void uploadBits(String imageUuid, PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, ReturnValueCompletion<String> completion);

    String makeRootVolumeTemplateInstallPath(String backupStorageUuid, String imageUuid);

    String makeVolumeSnapshotInstallPath(String backupStorageUuid, String snapshotUuid);

    String makeDataVolumeTemplateInstallPath(String backupStorageUuid, String imageUuid);
}
