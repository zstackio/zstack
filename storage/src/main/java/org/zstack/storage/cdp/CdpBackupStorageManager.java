package org.zstack.storage.cdp;

import org.zstack.header.Service;
import org.zstack.header.storage.backup.BackupStorageAllocatorStrategyFactory;
import org.zstack.header.storage.backup.BackupStorageFactory;
import org.zstack.header.storage.backup.BackupStorageType;

public interface CdpBackupStorageManager {
    BackupStorageFactory getBackupStorageFactory(BackupStorageType type);

    BackupStorageAllocatorStrategyFactory getAllocatorFactory(String type);
}