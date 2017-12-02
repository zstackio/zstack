package org.zstack.storage.ceph.backup;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStoragePrimaryStorageExtensionPoint;
import org.zstack.utils.CollectionDSL;

import java.util.List;

/**
 * Created by mingjian.deng on 2017/10/31.
 */
public class CephBackupStoragePrimaryStorageExtension implements BackupStoragePrimaryStorageExtensionPoint {
    @Override
    public String getPrimaryStoragePriorityMap(BackupStorageInventory bs, ImageInventory image) {
        if (!bs.getType().equals("Ceph")) {
            return null;
        }
        return CephBackupStorageGlobalProperty.CEPH_BACKUPSTORAGE_PRIORITY;
    }

    @Override
    public List<String> getExcludePrimaryStorageTypeList(BackupStorageInventory bs, ImageInventory image) {
        return CollectionDSL.list();
    }
}
