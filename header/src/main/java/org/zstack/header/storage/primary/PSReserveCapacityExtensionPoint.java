package org.zstack.header.storage.primary;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface PSReserveCapacityExtensionPoint {
    String getInstallUrl(String primaryStorageUuid);

    @Transactional(propagation = Propagation.MANDATORY)
    void reserveCapacity(String installUrl, long size, String psUuid);

    void releaseCapacity(String installUrl, long size, String psUuid);
}
