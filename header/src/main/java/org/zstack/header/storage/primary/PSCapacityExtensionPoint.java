package org.zstack.header.storage.primary;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface PSCapacityExtensionPoint {
    String buildAllocatedInstallUrl(AllocatePrimaryStorageSpaceMsg msg);

    @Transactional(propagation = Propagation.MANDATORY)
    String reserveCapacity(String installUrl, long size, String psUuid);

    String releaseCapacity(String installUrl, long size, String psUuid);

    String psCapacityPrimaryStorageType();
}
