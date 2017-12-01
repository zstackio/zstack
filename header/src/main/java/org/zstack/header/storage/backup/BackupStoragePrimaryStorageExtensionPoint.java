package org.zstack.header.storage.backup;

import org.zstack.header.image.ImageInventory;

import java.util.List;

/**
 * Created by mingjian.deng on 2017/10/31.
 */
public interface BackupStoragePrimaryStorageExtensionPoint {
    String getPrimaryStoragePriorityMap(final BackupStorageInventory bs, final ImageInventory image);

    List<String> getExcludePrimaryStorageTypeList(final BackupStorageInventory bs, final ImageInventory image);
}
