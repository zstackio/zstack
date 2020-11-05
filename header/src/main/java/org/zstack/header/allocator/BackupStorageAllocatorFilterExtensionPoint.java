package org.zstack.header.allocator;

import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.List;

public interface BackupStorageAllocatorFilterExtensionPoint {
    List<BackupStorageInventory> afterAllocatorBackupStorage(List<BackupStorageInventory> candidates, PrimaryStorageVO ps);
}
