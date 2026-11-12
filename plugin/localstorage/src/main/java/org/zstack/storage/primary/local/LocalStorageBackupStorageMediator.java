package org.zstack.storage.primary.local;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.ImageDownloadPurpose;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryToBackupStorageMediator;

/**
 * Created by frank on 7/1/2015.
 */
public interface LocalStorageBackupStorageMediator extends PrimaryToBackupStorageMediator {
    void downloadBits(PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, String hostUuid, boolean isData, Completion completion);

    default void downloadBits(String imageUuid, PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, String hostUuid, ImageDownloadPurpose purpose, Completion completion) {
        downloadBits(pinv, bsinv, backupStorageInstallPath, primaryStorageInstallPath, hostUuid, purpose == ImageDownloadPurpose.DataVolume, completion);
    }

    void uploadBits(String imageUuid, PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, String hostUuid, ReturnValueCompletion<String> completion);
}
