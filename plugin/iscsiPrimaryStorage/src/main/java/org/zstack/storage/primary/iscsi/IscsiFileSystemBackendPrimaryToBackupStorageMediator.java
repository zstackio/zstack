package org.zstack.storage.primary.iscsi;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryToBackupStorageMediator;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by frank on 4/19/2015.
 */
public interface IscsiFileSystemBackendPrimaryToBackupStorageMediator extends PrimaryToBackupStorageMediator{
    void createVolumeFromImageCache(PrimaryStorageInventory primaryStorage, ImageCacheInventory image,
                                    VolumeInventory volume, ReturnValueCompletion<String> completion);

    void downloadBits(PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, Completion completion);

    void uploadBits(PrimaryStorageInventory pinv, BackupStorageInventory bsinv, String backupStorageInstallPath, String primaryStorageInstallPath, Completion completion);

    String makeRootVolumeTemplateInstallPath(BackupStorageInventory bs, String imageUuid);

    String makeVolumeSnapshotInstallPath(BackupStorageInventory bs, String snapshotUuid);

    String makeDataVolumeTemplateInstallPath(BackupStorageInventory bs, String imageUuid);
}
