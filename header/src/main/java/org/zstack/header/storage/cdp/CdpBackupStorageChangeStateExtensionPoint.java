package org.zstack.header.storage.cdp;

import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageStateEvent;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageException;

public interface CdpBackupStorageChangeStateExtensionPoint {
    void preChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState nextState) throws BackupStorageException;

    void beforeChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState nextState);

    void afterChangeSecondaryStorageState(BackupStorageInventory inv, BackupStorageStateEvent evt, BackupStorageState previousState);
}
