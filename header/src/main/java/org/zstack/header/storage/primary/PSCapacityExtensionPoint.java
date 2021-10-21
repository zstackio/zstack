package org.zstack.header.storage.primary;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface PSCapacityExtensionPoint {
    String buildAllocatedInstallUrl(AllocatePrimaryStorageSpaceMsg msg, PrimaryStorageInventory psInv);

    @Transactional(propagation = Propagation.MANDATORY)
    String reserveCapacity(String allocatedInstallUrl, long size, String psUuid, boolean force);
    @Transactional(propagation = Propagation.MANDATORY)
    String releaseCapacity(String allocatedInstallUrl, long size, String psUuid);

    PrimaryStorageType getPrimaryStorageType();
}
