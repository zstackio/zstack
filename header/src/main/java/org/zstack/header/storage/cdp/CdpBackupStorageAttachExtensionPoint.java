package org.zstack.header.storage.cdp;

import org.zstack.header.storage.backup.BackupStorageInventory;

public interface CdpBackupStorageAttachExtensionPoint {
    String preAttachCdpBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void beforeAttachCdpBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void failToAttachCdpBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void afterAttachCdpBackupStorage(BackupStorageInventory inventory, String zoneUuid);
}
