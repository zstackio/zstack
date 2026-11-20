package org.zstack.header.storage.primary;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 10/21/2015.
 */
public interface RecalculatePrimaryStorageCapacityExtensionPoint {
    String getPrimaryStorageTypeForRecalculateCapacityExtensionPoint();

    default Map<String, Long> getSnapshotsTotalSizeByPrimaryStorage(List<String> primaryStorageUuids) {
        return Collections.emptyMap();
    }

    void afterRecalculatePrimaryStorageCapacity(RecalculatePrimaryStorageCapacityStruct struct);

    void beforeRecalculatePrimaryStorageCapacity(RecalculatePrimaryStorageCapacityStruct struct);
}
