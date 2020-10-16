package org.zstack.header.storage.primary;

import java.util.List;
import java.util.Map;

/**
 * Created by Wenhao.Zhang on 20/10/16
 */
public interface StorageTypeService {
    void setBackupStoragePrimaryStorageMetrics(Map<String, List<String>> metrics);
    Map<String, List<String>> getBackupStoragePrimaryStorageMetrics();
}
