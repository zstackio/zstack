package org.zstack.storage.ceph.backup;

import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStoragePrimaryStorageExtensionPoint;

/**
 * Created by mingjian.deng on 2017/10/31.
 */
public class CephBackupStoragePrimaryStorageExtension implements BackupStoragePrimaryStorageExtensionPoint {
    @Override
    public String getPrimaryStoragePriorityMap(BackupStorageInventory inv) {
        if (!inv.getType().equals("Ceph")) {
            return null;
        }
        return CephBackupStorageGlobalProperty.CEPH_BACKUPSTORAGE_PRIORITY;
    }
}
