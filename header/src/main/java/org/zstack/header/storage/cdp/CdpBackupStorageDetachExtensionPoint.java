package org.zstack.header.storage.cdp;

        import org.zstack.header.storage.backup.BackupStorageInventory;
        import org.zstack.header.storage.backup.BackupStorageException;

public interface CdpBackupStorageDetachExtensionPoint {
    void preDetachCdpBackupStorage(BackupStorageInventory inventory, String zoneUuid) throws BackupStorageException;

    void beforeDetachCdpBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void failToDetachCdpBackupStorage(BackupStorageInventory inventory, String zoneUuid);

    void afterDetachCdpBackupStorage(BackupStorageInventory inventory, String zoneUuid);
}
