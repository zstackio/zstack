package org.zstack.core.trash;

import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.header.storage.backup.StorageTrashSpec;

import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 2018/12/13.
 */
public interface StorageTrash {
    JsonLabelInventory createTrash(TrashType type, StorageTrashSpec spec);

    Map<String, StorageTrashSpec> getTrashList(String storageUuid);
    Map<String, StorageTrashSpec>  getTrashList(String storageUuid, List<TrashType> types);

    void remove(String trashKey, String storageUuid);  // only remove db, not storage data
}
