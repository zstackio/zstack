package org.zstack.header.storage.primary;

import java.util.List;
import java.util.Map;

public interface PrimaryStorageManager {
    PrimaryStorageFactory getPrimaryStorageFactory(PrimaryStorageType type);

    Map<PrimaryStorageHostStatus, List<String>> getHostUuidsByConnectionStatus(String primaryStorageUuid);

    String getPrimaryStorageType(String primaryStorageUuid);

    List<String> getAttachedClusterUuids(String primaryStorageUuid);
}
