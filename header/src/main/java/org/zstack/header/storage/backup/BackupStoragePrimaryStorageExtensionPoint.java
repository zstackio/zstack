package org.zstack.header.storage.backup;

/**
 * Created by mingjian.deng on 2017/10/31.
 */
public interface BackupStoragePrimaryStorageExtensionPoint {
    String getPrimaryStoragePriorityMap(final BackupStorageInventory inv);
}
