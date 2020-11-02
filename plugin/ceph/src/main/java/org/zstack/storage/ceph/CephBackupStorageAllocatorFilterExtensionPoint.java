package org.zstack.storage.ceph;

import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import java.util.List;

public interface CephBackupStorageAllocatorFilterExtensionPoint {
        List<BackupStorageInventory> filterBackupStorageCandidates(List<BackupStorageInventory> candidates, PrimaryStorageVO ps);
}
