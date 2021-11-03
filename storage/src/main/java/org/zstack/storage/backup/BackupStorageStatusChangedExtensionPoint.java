package org.zstack.storage.backup;

import org.zstack.header.storage.backup.BackupStorageStatus;

public interface BackupStorageStatusChangedExtensionPoint {
    void backupStorageStateChanged(String bsUuid, BackupStorageStatus oldState, BackupStorageStatus newState);
}
