package org.zstack.header.storage.cdp;

import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageException;

public interface CdpBackupStorageDeleteExtensionPoint {
    void preDeleteSecondaryStorage(BackupStorageInventory inv) throws BackupStorageException;

    void beforeDeleteSecondaryStorage(BackupStorageInventory inv);

    void afterDeleteSecondaryStorage(BackupStorageInventory inv);
}
